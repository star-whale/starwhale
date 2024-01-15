from __future__ import annotations

import typing as t
import dataclasses

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
from torch.utils.data import ChainDataset
from transformers.training_args import TrainingArguments as HFTrainingArguments

from starwhale import Dataset, argument, finetune

try:
    from .utils import BASE_MODEL_DIR, ADAPTER_MODEL_DIR
    from .evaluation import copilot_predict
except ImportError:
    from utils import BASE_MODEL_DIR, ADAPTER_MODEL_DIR
    from evaluation import copilot_predict

# fork from https://github.com/baichuan-inc/Baichuan2/blob/main/fine-tune/fine-tune.py

torch.backends.cuda.matmul.allow_tf32 = True


@dataclasses.dataclass
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


@dataclasses.dataclass
class ModelArguments:
    model_max_length: int = dataclasses.field(
        default=512, metadata={"help": "max length of generated text"}
    )


@dataclasses.dataclass
class TrainingArguments(HFTrainingArguments):
    # change default value
    # copy from https://github.com/baichuan-inc/Baichuan2/blob/main/README.md#%E5%8D%95%E6%9C%BA%E8%AE%AD%E7%BB%83
    output_dir: str = dataclasses.field(
        default=str(ADAPTER_MODEL_DIR), metadata={"help": "output dir"}
    )
    optim: str = dataclasses.field(
        default="adamw_torch", metadata={"help": "optimizer"}
    )
    report_to: str = dataclasses.field(
        default="none", metadata={"help": "report metrics to some service"}
    )
    num_train_epochs: int = dataclasses.field(default=2, metadata={"help": "epochs"})
    max_steps: int = dataclasses.field(default=18, metadata={"help": "max steps"})
    per_device_train_batch_size: int = dataclasses.field(
        default=2, metadata={"help": "per device train batch size"}
    )
    gradient_accumulation_steps: int = dataclasses.field(
        default=16, metadata={"help": "gradient accumulation steps"}
    )
    save_strategy: str = dataclasses.field(
        default="no", metadata={"help": "save strategy"}
    )
    learning_rate: float = dataclasses.field(
        default=2e-5, metadata={"help": "learning rate"}
    )
    lr_scheduler_type: str = dataclasses.field(
        default="constant", metadata={"help": "lr scheduler type"}
    )
    adam_beta1: float = dataclasses.field(default=0.9, metadata={"help": "adam beta1"})
    adam_beta2: float = dataclasses.field(default=0.98, metadata={"help": "adam beta2"})
    adam_epsilon: float = dataclasses.field(
        default=1e-8, metadata={"help": "adam epsilon"}
    )
    max_grad_norm: float = dataclasses.field(
        default=1.0, metadata={"help": "max grad norm"}
    )
    weight_decay: float = dataclasses.field(
        default=1e-4, metadata={"help": "weight decay"}
    )
    warmup_ratio: float = dataclasses.field(
        default=0.0, metadata={"help": "warmup ratio"}
    )
    logging_steps: int = dataclasses.field(
        default=10, metadata={"help": "logging steps"}
    )
    gradient_checkpointing: bool = dataclasses.field(
        default=False, metadata={"help": "gradient checkpointing"}
    )
    remove_unused_columns: bool = dataclasses.field(
        default=False, metadata={"help": "remove unused columns"}
    )


@argument((ModelArguments, TrainingArguments))
@finetune(
    resources={"nvidia.com/gpu": 1},
    require_train_datasets=True,
    model_modules=[copilot_predict, "finetune:lora_finetune"],
)
def lora_finetune(
    train_datasets: t.List[Dataset],
    arguments: t.Tuple[ModelArguments, HFTrainingArguments],
) -> None:
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

    model_arguments, train_arguments = arguments

    tokenizer = AutoTokenizer.from_pretrained(
        BASE_MODEL_DIR,
        use_fast=False,
        trust_remote_code=True,
        model_max_length=model_arguments.model_max_length,
    )

    train_dataset = ChainDataset(
        [
            ds.to_pytorch(
                transform=DataCollatorForCausalLM(
                    tokenizer=tokenizer, source_max_len=16, target_max_len=512
                )
            )
            for ds in train_datasets
        ]
    )

    trainer = Trainer(
        model=model,
        tokenizer=tokenizer,
        args=train_arguments,
        train_dataset=train_dataset,
    )

    print("Starting model training...")
    train_result = trainer.train(resume_from_checkpoint=None)
    print(train_result.metrics)
    trainer.save_state()
    trainer.save_model()
