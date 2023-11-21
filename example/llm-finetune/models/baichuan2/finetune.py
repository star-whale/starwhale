from __future__ import annotations

import os
import typing as t
from dataclasses import dataclass

import torch
from peft import (
    TaskType,
    PeftModel,
    LoraConfig,
    get_peft_model,
    prepare_model_for_kbit_training,
)
from transformers import (
    Trainer,
    AutoTokenizer,
    BitsAndBytesConfig,
    PreTrainedTokenizer,
    AutoModelForCausalLM,
)
from transformers.training_args import TrainingArguments

from starwhale import dataset, finetune

try:
    from .utils import BASE_MODEL_DIR, ADAPTER_MODEL_DIR
    from .evaluation import copilot_predict
except ImportError:
    from utils import BASE_MODEL_DIR, ADAPTER_MODEL_DIR
    from evaluation import copilot_predict

# fork from https://github.com/baichuan-inc/Baichuan2/blob/main/fine-tune/fine-tune.py

torch.backends.cuda.matmul.allow_tf32 = True


@dataclass
class DataCollatorForCausalLM:
    tokenizer: PreTrainedTokenizer
    source_max_len: int
    target_max_len: int

    user_tokens = [195]
    assistant_tokens = [196]
    ignore_index = -100

    def __call__(self, example: t.Dict) -> t.Dict:
        input_ids = []
        labels = []

        for message in example["conversations"]:
            from_ = message["from"]
            value = message["value"]
            value_ids = self.tokenizer.encode(value)

            if from_ == "human":
                input_ids += self.user_tokens + value_ids
                labels += [self.tokenizer.eos_token_id] + [self.ignore_index] * len(
                    value_ids
                )
            else:
                input_ids += self.assistant_tokens + value_ids
                labels += [self.ignore_index] + value_ids
        input_ids.append(self.tokenizer.eos_token_id)
        labels.append(self.tokenizer.eos_token_id)
        input_ids = input_ids[: self.tokenizer.model_max_length]
        labels = labels[: self.tokenizer.model_max_length]
        input_ids += [self.tokenizer.pad_token_id] * (
            self.tokenizer.model_max_length - len(input_ids)
        )
        labels += [self.ignore_index] * (self.tokenizer.model_max_length - len(labels))
        input_ids = torch.LongTensor(input_ids)
        labels = torch.LongTensor(labels)
        attention_mask = input_ids.ne(self.tokenizer.pad_token_id)
        return {
            "input_ids": input_ids,
            "labels": labels,
            "attention_mask": attention_mask,
        }


@finetune(
    resources={"nvidia.com/gpu": 1},
    require_train_datasets=True,
    model_modules=[copilot_predict, "finetune:lora_finetune"],
)
def lora_finetune(train_datasets: t.List[str]) -> None:
    # TODO: support multi train datasets
    train_dataset = train_datasets[0]
    if isinstance(train_dataset, str):
        train_dataset = dataset(train_dataset, readonly=True)

    model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL_DIR,
        trust_remote_code=True,
        torch_dtype=torch.float16,
        device_map="auto",  # for multi-gpus
        load_in_4bit=True,  # for lower gpu memory usage
        quantization_config=BitsAndBytesConfig(
            load_in_4bit=True,
            llm_int8_threshold=6.0,
            llm_int8_has_fp16_weight=False,
            bnb_4bit_compute_dtype=torch.float16,
            bnb_4bit_use_double_quant=True,
            bnb_4bit_quant_type="nf4",
        ),
    )
    model.model_parallel = True
    model.is_parallelizable = True
    model.config.torch_dtype = torch.float16
    model = prepare_model_for_kbit_training(model)

    # support finetune a lora-finetuned model
    if (ADAPTER_MODEL_DIR / "adapter_config.json").exists():
        print(f"loading adapters {ADAPTER_MODEL_DIR}...")
        model = PeftModel.from_pretrained(
            model,
            str(ADAPTER_MODEL_DIR),
            is_trainable=True,
        )
    else:
        print("init model with peft lora config...")
        peft_config = LoraConfig(
            task_type=TaskType.CAUSAL_LM,
            target_modules=["W_pack"],
            inference_mode=False,
            r=64,
            lora_alpha=16,
            bias="none",
            lora_dropout=0.05,
        )
        model = get_peft_model(model, peft_config)
    model.print_trainable_parameters()

    for name, module in model.named_modules():
        if "norm" in name:
            module = module.to(torch.float32)

    tokenizer = AutoTokenizer.from_pretrained(
        BASE_MODEL_DIR,
        use_fast=False,
        trust_remote_code=True,
        model_max_length=int(os.environ.get("MODEL_MAX_LENGTH", 512)),
    )

    # TODO: support finetune arguments
    # copy from https://github.com/baichuan-inc/Baichuan2/blob/main/README.md#%E5%8D%95%E6%9C%BA%E8%AE%AD%E7%BB%83
    train_args = TrainingArguments(
        output_dir=str(ADAPTER_MODEL_DIR),
        optim="adamw_torch",
        report_to="none",
        num_train_epochs=int(os.environ.get("NUM_TRAIN_EPOCHS", 2)),
        max_steps=int(os.environ.get("MAX_STEPS", 18)),
        per_device_train_batch_size=2,  # more batch size will cause OOM
        gradient_accumulation_steps=16,
        save_strategy="no",  # no need to save checkpoint for finetune
        learning_rate=2e-5,
        lr_scheduler_type="constant",
        adam_beta1=0.9,
        adam_beta2=0.98,
        adam_epsilon=1e-8,
        max_grad_norm=1.0,
        weight_decay=1e-4,
        warmup_ratio=0.0,
        logging_steps=10,
        gradient_checkpointing=False,
        remove_unused_columns=False,
    )

    # TODO: support deepspeed

    trainer = Trainer(
        model=model,
        tokenizer=tokenizer,
        args=train_args,
        train_dataset=train_dataset.to_pytorch(
            transform=DataCollatorForCausalLM(
                tokenizer=tokenizer, source_max_len=16, target_max_len=512
            )
        ),
    )

    print("Starting model training...")
    train_result = trainer.train(resume_from_checkpoint=None)
    print(train_result.metrics)
    trainer.save_state()
    trainer.save_model()
