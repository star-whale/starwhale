from __future__ import annotations

import os
import copy
import typing as t
from pathlib import Path
from dataclasses import dataclass

import torch
import bitsandbytes as bnb
import transformers
from peft import PeftModel, LoraConfig, get_peft_model, prepare_model_for_kbit_training
from transformers import (
    set_seed,
    AutoTokenizer,
    Seq2SeqTrainer,
    BitsAndBytesConfig,
    AutoModelForCausalLM,
    Seq2SeqTrainingArguments,
)
from peft.tuners.lora import LoraLayer
from torch.nn.utils.rnn import pad_sequence

from starwhale import Dataset, finetune

try:
    from .utils import (
        get_model_name,
        prepare_model_package,
        get_base_and_adapter_model_path,
    )
    from .evaluation import copilot_predict
except ImportError:
    from utils import (
        get_model_name,
        prepare_model_package,
        get_base_and_adapter_model_path,
    )
    from evaluation import copilot_predict

torch.backends.cuda.matmul.allow_tf32 = True
default_compute_dtype = torch.float16  # only A100 supports bfloat16


IGNORE_INDEX = -100
DEFAULT_PAD_TOKEN = "[PAD]"
os.environ["CUDA_LAUNCH_BLOCKING"] = "1"
max_train_steps = os.environ.get("MAX_TRAIN_STEPS", 18)  # 1875
max_eval_steps = os.environ.get("MAX_EVAL_STEPS", 1)  # 187


@finetune(
    resources={"nvidia.com/gpu": 1},
    require_train_datasets=True,
    require_validation_datasets=True,
    model_modules=[copilot_predict],
)
def llama_fine_tuning(train_datasets: t.List[Dataset], eval_datasets: t.List[Dataset]):
    # TODO: support multiple datasets
    train_llama(train_dataset=train_datasets[0], eval_dataset=eval_datasets[0])
    model_name = get_model_name()
    prepare_model_package(model_name, skip_adapter=False)


def get_accelerate_model(
    base_model_path: Path,
    adapter_model_path: Path,
    bits: int = 4,
    compute_dtype: torch.dtype = default_compute_dtype,
) -> transformers.PreTrainedModel:
    print(f"loading base model: {base_model_path}")
    load_in_4bit = bits == 4
    load_in_8bit = bits == 8

    if compute_dtype == torch.bfloat16:
        torch_dtype = torch.bfloat16
    else:
        torch_dtype = torch.float32

    # https://huggingface.co/blog/4bit-transformers-bitsandbytes
    # QLoRA = nf4 + double quantization + load in 4bit
    model = AutoModelForCausalLM.from_pretrained(
        str(base_model_path),
        load_in_4bit=load_in_4bit,
        load_in_8bit=load_in_8bit,
        device_map="auto",  # make trainer use multi gpu devices
        quantization_config=BitsAndBytesConfig(
            load_in_4bit=load_in_4bit,
            load_in_8bit=load_in_8bit,
            llm_int8_threshold=6.0,
            llm_int8_has_fp16_weight=False,
            bnb_4bit_compute_dtype=compute_dtype,
            bnb_4bit_use_double_quant=True,
            bnb_4bit_quant_type="nf4",
        ),
        torch_dtype=torch_dtype,
    )
    model.model_parallel = True
    model.is_parallelizable = True
    model.config.torch_dtype = torch_dtype
    model = prepare_model_for_kbit_training(model, use_gradient_checkpointing=True)
    model.gradient_checkpointing_enable()

    print(f"Loading adapters {adapter_model_path}...")
    # make continue training possible: base model -> adapter model1 -> adapter model2
    if adapter_model_path.exists():
        model = PeftModel.from_pretrained(
            model, str(adapter_model_path), is_trainable=True
        )
    else:
        modules = find_all_linear_names(bits, model)
        config = LoraConfig(
            r=64,
            lora_alpha=16,
            target_modules=modules,
            lora_dropout=0.05,
            bias="none",
            task_type="CAUSAL_LM",
        )
        model = get_peft_model(model, config)

    for name, module in model.named_modules():
        if isinstance(module, LoraLayer):
            if compute_dtype == torch.bfloat16:
                module = module.to(torch.bfloat16)

        if "norm" in name:
            module = module.to(torch.float32)

        if "lm_head" in name or "embed_tokens" in name:
            if (
                hasattr(module, "weight")
                and compute_dtype == torch.bfloat16
                and module.weight.dtype == torch.float32
            ):
                print(f"--> name:{name} to bfloat16")
                module = module.to(torch.bfloat16)

    model.config.use_cache = False
    _print_trainable_parameters(bits, model)
    return model


def train_llama(
    train_dataset: Dataset,
    eval_dataset: t.Optional[Dataset] = None,
) -> None:
    base_model_path, adapter_model_path = get_base_and_adapter_model_path()
    model = get_accelerate_model(
        base_model_path=base_model_path,
        adapter_model_path=adapter_model_path,
        bits=4,
        compute_dtype=default_compute_dtype,
    )
    print("model loaded")
    set_seed(0)

    tokenizer = AutoTokenizer.from_pretrained(
        base_model_path,
        padding_side="right",
        use_fast=False,  # Fast tokenizer giving issues.
        tokenizer_type="llama",
    )
    if tokenizer.pad_token is None:
        _smart_tokenizer_and_embedding_resize(
            special_tokens_dict=dict(pad_token=DEFAULT_PAD_TOKEN),
            tokenizer=tokenizer,
            model=model,
        )
    print("Adding special tokens to the tokenizer...")
    # LLaMA tokenizer may not have correct special tokens set.
    # Check and add them if missing to prevent them from being parsed into different tokens.
    # Note that these are present in the vocabulary.
    # Note also that `model.config.pad_token_id` is 0 which corresponds to `<unk>` token.
    tokenizer.add_special_tokens(
        {
            "eos_token": tokenizer.convert_ids_to_tokens(model.config.eos_token_id),
            "bos_token": tokenizer.convert_ids_to_tokens(model.config.bos_token_id),
            "unk_token": tokenizer.convert_ids_to_tokens(
                model.config.pad_token_id
                if model.config.pad_token_id != -1
                else tokenizer.pad_token_id
            ),
        }
    )

    training_args = Seq2SeqTrainingArguments(
        output_dir=adapter_model_path,
        logging_steps=10,
        save_strategy="no",  # no need to save checkpoint for fine-tuning
        evaluation_strategy="epoch",
        weight_decay=0.0,
        learning_rate=0.0001,
        max_steps=max_train_steps,
        eval_steps=max_eval_steps,
        max_grad_norm=0.3,
        gradient_accumulation_steps=16,
        per_device_eval_batch_size=1,
        warmup_ratio=0.03,
        lr_scheduler_type="constant",
        gradient_checkpointing=False,
        remove_unused_columns=False,
        optim="paged_adamw_32bit",
        adam_beta2=0.999,
    )
    training_args.generation_config = transformers.GenerationConfig(
        max_new_tokens=32,
        do_sample=False,
        num_beams=1,
        num_beam_groups=1,
        temperature=1.0,
        top_k=50,
        top_p=1.0,
        typical_p=1.0,
        diversity_penalty=0.0,
        repetition_penalty=1.0,
        length_penalty=1.0,
        no_repeat_ngram_size=0,
    )

    trainer = Seq2SeqTrainer(
        model=model,
        tokenizer=tokenizer,
        train_dataset=train_dataset.to_pytorch(),
        eval_dataset=eval_dataset.to_pytorch() if eval_dataset else None,
        data_collator=DataCollatorForCausalLM(
            tokenizer=tokenizer,
            source_max_len=16,
            target_max_len=512,
        ),
        args=training_args,
    )

    print("Starting model training...")
    train_result = trainer.train(resume_from_checkpoint=None)
    print(train_result.metrics)
    trainer.save_state()
    if eval_dataset is not None:
        metrics = trainer.evaluate(metric_key_prefix="eval")
        print(metrics)

    model.save_pretrained(adapter_model_path)


def _print_trainable_parameters(bits: int, model) -> None:
    trainable_params = 0
    all_param = 0
    for _, param in model.named_parameters():
        all_param += param.numel()
        if param.requires_grad:
            trainable_params += param.numel()
    if bits == 4:
        trainable_params /= 2
    print(
        f"trainable params: {trainable_params} || "
        f"all params: {all_param} || "
        f"trainable: {100 * trainable_params / all_param}"
    )


def _smart_tokenizer_and_embedding_resize(
    special_tokens_dict: t.Dict,
    tokenizer: transformers.PreTrainedTokenizer,
    model: transformers.PreTrainedModel,
) -> None:
    """Resize tokenizer and embedding.

    Note: This is the unoptimized version that may make your embedding size not be divisible by 64.
    """
    num_new_tokens = tokenizer.add_special_tokens(special_tokens_dict)
    model.resize_token_embeddings(len(tokenizer))

    if num_new_tokens > 0:
        input_embeddings = model.get_input_embeddings().weight.data
        output_embeddings = model.get_output_embeddings().weight.data

        input_embeddings_avg = input_embeddings[:-num_new_tokens].mean(
            dim=0, keepdim=True
        )
        output_embeddings_avg = output_embeddings[:-num_new_tokens].mean(
            dim=0, keepdim=True
        )

        input_embeddings[-num_new_tokens:] = input_embeddings_avg
        output_embeddings[-num_new_tokens:] = output_embeddings_avg


@dataclass
class DataCollatorForCausalLM:
    tokenizer: transformers.PreTrainedTokenizer
    source_max_len: int
    target_max_len: int

    def __call__(self, instances: t.Sequence[t.Dict]) -> t.Dict[str, torch.Tensor]:
        # Extract elements
        sources = [
            f"{self.tokenizer.bos_token}{example.get('input', '')}"
            for example in instances
        ]

        def _fetch_output(example: t.Dict) -> str:
            if "text" in example:
                return example["text"]
            return example["output"]

        targets = [
            f"{_fetch_output(example)}{self.tokenizer.eos_token}"
            for example in instances
        ]
        # Tokenize
        tokenized_sources_with_prompt = self.tokenizer(
            sources,
            max_length=self.source_max_len,
            truncation=True,
            add_special_tokens=False,
        )
        tokenized_targets = self.tokenizer(
            targets,
            max_length=self.target_max_len,
            truncation=True,
            add_special_tokens=False,
        )
        # Build the input and labels for causal LM
        input_ids = []
        labels = []
        for tokenized_source, tokenized_target in zip(
            tokenized_sources_with_prompt["input_ids"], tokenized_targets["input_ids"]
        ):
            input_ids.append(torch.tensor(tokenized_source + tokenized_target))
            labels.append(
                torch.tensor(
                    [IGNORE_INDEX for _ in range(len(tokenized_source))]
                    + copy.deepcopy(tokenized_target)
                )
            )
        # Apply padding
        input_ids = pad_sequence(
            input_ids, batch_first=True, padding_value=self.tokenizer.pad_token_id
        )
        labels = pad_sequence(labels, batch_first=True, padding_value=IGNORE_INDEX)
        data_dict = {
            "input_ids": input_ids,
            "attention_mask": input_ids.ne(self.tokenizer.pad_token_id),
        }
        if labels is not None:
            data_dict["labels"] = labels
        return data_dict


def find_all_linear_names(bits, model):
    cls = (
        bnb.nn.Linear4bit
        if bits == 4
        else (bnb.nn.Linear8bitLt if bits == 8 else torch.nn.Linear)
    )
    lora_module_names = set()
    for name, module in model.named_modules():
        if isinstance(module, cls):
            names = name.split(".")
            lora_module_names.add(names[0] if len(names) == 1 else names[-1])

    if "lm_head" in lora_module_names:  # needed for 16-bit
        lora_module_names.remove("lm_head")
    return list(lora_module_names)
