from __future__ import annotations

import os
import sys
import copy
import typing as t
from pathlib import Path
from functools import partial

import torch
import transformers
from peft import (
    LoraConfig,
    get_peft_model,
    get_peft_model_state_dict,
    prepare_model_for_int8_training,
)
from transformers import (
    Trainer,
    set_seed,
    AutoTokenizer,
    TrainingArguments,
    AutoModelForCausalLM,
    DataCollatorForSeq2Seq,
)

from starwhale import model as StarwhaleModel
from starwhale import Context, dataset
from starwhale import Dataset as StarwhaleDataset
from starwhale import fine_tune

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"

transformers.utils.logging.set_verbosity(transformers.logging.DEBUG)
transformers.utils.logging.enable_default_handler()
transformers.utils.logging.enable_explicit_format()


@fine_tune(resources={"nvidia.com/gpu": 1})
def fine_tune_bloomz() -> None:
    ctx = Context.get_runtime_context()

    if len(ctx.dataset_uris) == 2:
        # TODO: use more graceful way to get train and eval dataset
        train_dataset = dataset(ctx.dataset_uris[0], readonly=True, create="forbid")
        eval_dataset = dataset(ctx.dataset_uris[1], readonly=True, create="forbid")
    elif len(ctx.dataset_uris) == 1:
        train_dataset = dataset(ctx.dataset_uris[0], readonly=True, create="forbid")
        eval_dataset = None
    else:
        # TODO: support multiple datasets
        raise ValueError("Only support 1 or 2 datasets(train and eval dataset) for now")

    train_bloomz_with_lora(
        pretrained_model=PRETRAINED_MODELS_DIR / "bloomz-7b1-mt",
        output_dir=PRETRAINED_MODELS_DIR / "lora",
        train_dataset=train_dataset,
        eval_dataset=eval_dataset,
    )
    StarwhaleModel.build(name="bloomz-lora-ft")


def train_bloomz_with_lora(
    pretrained_model: str | Path,
    output_dir: str | Path,
    train_dataset: StarwhaleDataset,
    eval_dataset: t.Optional[StarwhaleDataset] = None,
    torch_dtype: str | torch.dtype = torch.float16,
    world_size: int = 1,
    lora_r: int = 16,
    lora_alpha: int = 32,
    lora_output: float = 0.05,
    seed: int = 100,
    epochs: int = 3,
    learning_rate: float = 3e-4,
    max_model_input_size: int = 512,
) -> None:
    """
    Bloom with LoRA and int8 optimization. The train code is from https://github.com/LianjiaTech/BELLE/blob/main/train/src/train.py.

    Arguments:
        torch_dtype: torch dtype, choices: "auto", torch.float32", torch.float16", torch.bfloat16.
    """
    pretrained_model = str(pretrained_model)
    output_dir = str(output_dir)

    if world_size == 1:
        device_map = "auto"
    else:
        device_map = {"": int(os.environ.get("LOCAL_RANK", 0))}

    set_seed(seed)

    tokenizer = AutoTokenizer.from_pretrained(pretrained_model)
    tokenizer.pad_token_id = 0  # we want this to be different from the eos token
    tokenizer.padding_side = "left"  # Allow batched inference

    model = AutoModelForCausalLM.from_pretrained(
        pretrained_model,
        load_in_8bit=True,
        torch_dtype=torch_dtype,
        device_map=device_map,
    )

    lora_config = LoraConfig(
        r=lora_r,
        lora_alpha=lora_alpha,
        target_modules=["query_key_value"],
        lora_dropout=lora_output,
        bias="none",
        task_type="CAUSAL_LM",
    )
    model = prepare_model_for_int8_training(model)
    if hasattr(model, "enable_input_require_grads"):
        model.enable_input_require_grads()
    else:
        model.get_input_embeddings().register_forward_hook(lambda module, input, output: output.requires_grad_(True))  # type: ignore

    model = get_peft_model(model, lora_config)
    model.print_trainable_parameters()

    if world_size != 1 and torch.cuda.device_count() > 1:
        model.is_parallelizable = True
        model.model_parallel = True

    transform_prompt = partial(
        _transform_data_point, tokenizer=tokenizer, max_input_size=max_model_input_size
    )
    trainer = Trainer(
        model=model,
        train_dataset=train_dataset.to_pytorch(transform=transform_prompt),
        eval_dataset=eval_dataset.to_pytorch(transform=transform_prompt)
        if eval_dataset
        else None,
        args=TrainingArguments(
            per_device_train_batch_size=4,
            output_dir=output_dir,
            warmup_steps=100,
            eval_steps=200,
            save_steps=200,
            num_train_epochs=epochs,
            fp16=False,
            learning_rate=learning_rate,
            save_total_limit=3,
        ),
        data_collator=DataCollatorForSeq2Seq(
            tokenizer, pad_to_multiple_of=8, return_tensors="pt", padding=True
        ),
    )
    model.config.use_cache = False
    _old_state_dict = model.state_dict
    model.state_dict = (
        lambda self, *_, **__: get_peft_model_state_dict(self, _old_state_dict())
    ).__get__(model, type(model))
    if torch.__version__ >= "2" and sys.platform != "win32":
        model = torch.compile(model)
    trainer.train(resume_from_checkpoint=None)
    model.save_pretrained(output_dir)


_PROMPT_TEMPLATE = (
    "Below is an instruction that describes a task, please write a response that appropriately completes the request.\n\n"
    "### Instruction:\n{instruction}\n\n"
    "### Response:"
)

_IGNORE_INDEX = -100


def _transform_data_point(
    data_point: t.Dict,
    tokenizer: AutoTokenizer,
    max_input_size: int,
) -> t.Dict:
    instruct_ids = tokenizer.encode(
        _PROMPT_TEMPLATE.format(instruction=data_point["instruction"]),
        add_special_tokens=False,
    )
    output_ids = tokenizer.encode(data_point["output"], add_special_tokens=False)

    input_ids = instruct_ids + copy.deepcopy(output_ids)
    label_ids = [_IGNORE_INDEX] * len(instruct_ids) + output_ids

    input_ids = input_ids[: max_input_size - 1] + [tokenizer.eos_token_id]
    label_ids = label_ids[: max_input_size - 1] + [tokenizer.eos_token_id]
    return {
        "input_ids": input_ids,
        "attention_mask": [1] * len(input_ids),
        "labels": label_ids,
    }


def smoke_run():
    from peft import PeftModel
    from transformers import AutoTokenizer, AutoModelForCausalLM

    checkpoint = str(PRETRAINED_MODELS_DIR / "bloomz-7b1-mt")
    lora = str(PRETRAINED_MODELS_DIR / "lora")
    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    tokenizer.pad_token_id = 0
    tokenizer.bos_token_id = 1
    tokenizer.eos_token_id = 2
    tokenizer.padding_side = "left"

    model = AutoModelForCausalLM.from_pretrained(
        checkpoint,
        torch_dtype=torch.float16,
        device_map="auto",
    )
    model = PeftModel.from_pretrained(
        model, lora, torch_dtype=torch.float16, device_map={"": 0}
    )
    # model.eval()

    input_ids = tokenizer.encode(
        "please translate the following sentence to Chinese.\nI love to learn new things every day.\n",
        return_tensors="pt",
    )
    outputs = model.generate(
        input_ids=input_ids,
        temperature=0.001,
        top_k=30,
        top_p=0.85,
        do_sample=True,
        num_beams=1,
        repetition_penalty=1.2,
    )
    print(tokenizer.decode(outputs[0], skip_special_tokens=True))


if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) == 2 else "train"
    if action == "train":
        train_bloomz_with_lora(
            pretrained_model=PRETRAINED_MODELS_DIR / "bloomz-7b1-mt",
            output_dir=PRETRAINED_MODELS_DIR / "lora",
            train_dataset=dataset("belle-cn50k-train"),
            eval_dataset=dataset("belle-cn1k-eval"),
        )
    elif action == "smoke-run":
        smoke_run()
