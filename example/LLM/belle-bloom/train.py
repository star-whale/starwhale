from __future__ import annotations

import os
import sys
import copy
import typing as t
from pathlib import Path

import torch
from peft import (
    LoraConfig,
    get_peft_model,
    get_peft_model_state_dict,
    prepare_model_for_int8_training,
)
from transformers import (
    Trainer,
    AutoTokenizer,
    TrainingArguments,
    AutoModelForCausalLM,
    DataCollatorForSeq2Seq,
)

IGNORE_INDEX = -100
ROOTDIR = Path(__file__).parent


def train_bloom_with_lora(
    pretrained_model: str,
    train_dataset: t.Any,
    eval_dataset: t.Any,
    output_dir: str = str(ROOTDIR / "output"),
    torch_dtype: str | torch.dtype = "auto",
    world_size: int = 1,
    lora_r: int = 16,
    lora_alpha: int = 32,
    lora_output: float = 0.05,
    max_sequence_length: int = 512,
) -> None:
    """
    Bloom with LoRA and int8 optimization. The train code is from https://github.com/LianjiaTech/BELLE/blob/main/train/src/train.py.

    Arguments:
        torch_dtype: torch dtype, choices: "auto", torch.float32", torch.float16", torch.bfloat16.
    """

    if world_size == 1:
        device_map = "auto"
    else:
        device_map = {"": int(os.environ.get("LOCAL_RANK", 0))}

    model = AutoModelForCausalLM.from_pretrained(
        pretrained_model,
        load_in_8bit=True,
        torch_dtype=torch_dtype,
        device_map=device_map,
    )

    tokenizer = AutoTokenizer.from_pretrained(pretrained_model)
    tokenizer.pad_token_id = 0  # we want this to be different from the eos token
    tokenizer.padding_side = "left"  # Allow batched inference

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

    #     train_data = data["train"].shuffle().map(generate_and_tokenize_prompt)
    # val_data = load_dataset(
    #    "json", data_files=data_args.validation_file, cache_dir=model_args.cache_dir
    # )
    # val_data = val_data["train"].shuffle().map(generate_and_tokenize_prompt)

    trainer = Trainer(
        model=model,
        train_dataset=train_dataset,
        eval_dataset=eval_dataset,
        args=TrainingArguments(
            output_dir=output_dir,
            warmup_steps=100,
            eval_steps=200,
            save_steps=200,
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


def _generate_prompt(
    data_point: t.Dict, tokenizer: AutoTokenizer, max_sequence_length: int
) -> t.Dict:
    input_ids = []
    labels = []
    source = data_point["conversations"]
    for sentence in source:
        sentence_from = sentence["from"].lower()
        sentence_value = (
            "Human: \n" + sentence["value"] + "\n\nAssistant: \n"
            if sentence_from == "human"
            else sentence["value"]
        )  # https://github.com/LianjiaTech/BELLE/issues/337
        # conversation += sentence_value
        sentence_ids = tokenizer.encode(
            sentence_value, add_special_tokens=False
        )  # do not add bos_token_id
        label = (
            copy.deepcopy(sentence_ids)
            if sentence_from != "human"
            else [IGNORE_INDEX] * len(sentence_ids)
        )
        input_ids += sentence_ids
        labels += label
        # add eos at every end of assistant sentence
        if sentence_from != "human":
            input_ids += [tokenizer.eos_token_id]  # make sure eos_token_id is correct
            labels += [tokenizer.eos_token_id]

    input_ids = input_ids[: max_sequence_length - 1]
    labels = labels[: max_sequence_length - 1]
    if not any(x > -100 for x in labels):
        # labels can not have all values being -100. 18 and 24 are just random numbers
        labels[18:24] = input_ids[18:24]

    prompt = {
        "input_ids": input_ids,
        "attention_mask": [1] * len(input_ids),
        "labels": labels,
    }
    return prompt
