# pip install -q transformers accelerate starwhale
import os
from typing import Any
from pathlib import Path

import tqdm
import torch
from datasets import Dataset
from transformers import (
    Trainer,
    BloomForCausalLM,
    TrainingArguments,
    BloomTokenizerFast,
)

from starwhale import Context, dataset, fine_tune, evaluation, pass_context
from starwhale.api import model as swmp

ROOTDIR = Path(__file__).parent

tokenizer = None
model = None


@evaluation.predict(
    log_mode="plain",
    log_dataset_features=["query", "text", "question", "rawquestion", "prompt"],
    replicas=1,
)
def ppl(data: dict, external: dict):
    checkpoint = str(ROOTDIR / "models")
    if not os.path.exists(checkpoint):
        from download_model import download

        download(os.getenv("BLOOM_MODEL_NAME") or "bigscience/bloom-560m")
    global tokenizer
    if tokenizer is None:
        tokenizer = BloomTokenizerFast.from_pretrained(checkpoint)
    global model
    if model is None:
        model = BloomForCausalLM.from_pretrained(
            checkpoint, torch_dtype="auto", device_map="auto"
        )

    ds_name = external["dataset_uri"].name if external else ''
    if "text" in data:
        text = data["text"]
    elif "question" in data:
        text = data["question"]
    elif "rawquestion" in data:
        text = data["rawquestion"]
    elif "prompt" in data:
        text = data["prompt"]
    elif "query" in data:
        text = data["query"]
    else:
        raise ValueError(f"dataset {ds_name} does not fit this model")

    inputs = tokenizer.encode(text, return_tensors="pt").to("cuda")
    outputs = model.generate(inputs)
    result = tokenizer.decode(outputs[0])
    print(result)
    return result

@pass_context
@fine_tune()
def ft(context: Context) -> None:
    ft_inner(context=context)


ds_key_selectors = {
    "webqsp": {
        "rawquestion": "instruction",
        "parses[0].Answers[0].EntityName": "output",
    },
    "grailqav1": {"question": "instruction", "answer[0].entity_name": "output"},
    "graph_questions_testing": {"question": "instruction", "answer[0]": "output"},
    "z_bench_common": {"prompt": "instruction", "gpt4": "output"},
    "mkqa": {"query": "instruction", "answers.en[0].text": "output"},
}


def ft_inner(
    context: Context = None,
    swds: str = "mkqa/version/latest",
) -> None:
    checkpoint = str(ROOTDIR / "models")
    if not os.path.exists(checkpoint):
        from download_model import download

        download(os.getenv("BLOOM_MODEL_NAME") or "bigscience/bloom-560m")
    tokeniser = BloomTokenizerFast.from_pretrained(checkpoint, add_prefix_space=True)
    model = BloomForCausalLM.from_pretrained(
        checkpoint, torch_dtype="auto", device_map="auto"
    )

    swds_name = context.dataset_uris[0] if context else swds
    sw_dataset = dataset(swds_name, readonly=True, create="forbid")
    sw_dataset = sw_dataset.with_loader_config(
        field_transformer=ds_key_selectors.get(sw_dataset._uri.name, None)
    )
    hgds = swds2hgds(sw_dataset)
    input_ids = tokenise_data(hgds, tokeniser)

    model.gradient_checkpointing_enable()
    model.is_parallelizable = True
    model.model_parallel = True

    # train
    trainer = ModifiedTrainer(
        model=model,
        train_dataset=input_ids,
        args=TrainingArguments(
            output_dir=checkpoint,
            per_device_train_batch_size=int(os.getenv("BLOOM_TRAIN_BATCH_SIZE")) or 8,
            gradient_accumulation_steps=1,
            num_train_epochs=int(os.getenv("BLOOM_TRAIN_EPOCHES")) or 4,
            learning_rate=2e-5,
            fp16=True,
            logging_steps=10,
        ),
        data_collator=data_collator,
    )
    trainer.train()
    swmp.build(
        workdir=ROOTDIR,
        name=os.getenv("BLOOM_MODEL_NAME") or "bloom",
        modules=[ft],
    )


class ModifiedTrainer(Trainer):
    def compute_loss(self, model, inputs, return_outputs=False):
        return model(
            input_ids=inputs["input_ids"],
            attention_mask=torch.ones_like(inputs["input_ids"]).bool(),
            labels=inputs["input_ids"],
        ).loss


def data_collator(features: list) -> dict:
    return {"input_ids": torch.stack([torch.LongTensor(f) for f in features])}


def tokenise_data(dataset, tokenizer, max_seq_length=512):
    tokenised_list = []
    for elem in tqdm.tqdm(dataset):
        tokenised_list.append(
            tokenizer.encode(
                elem["text"],
                max_length=max_seq_length,
                padding="max_length",
                truncation=True,
            )
        )
    return tokenised_list


def swds2hgds(swds) -> Any:
    sw_ds = swds

    def my_gen():
        for item in sw_ds:
            feats = item.features
            instruction = feats["instruction"]
            output = feats["output"]
            yield {
                "text": f"given the question that {instruction}, please answer as ####{output}"
            }

    return Dataset.from_generator(my_gen)


if __name__ == "__main__":
    ft_inner()
