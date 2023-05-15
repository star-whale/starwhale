import os
from pathlib import Path
from collections import namedtuple

import torch
from peft import TaskType, LoraConfig, get_peft_model
from gradio import gradio
from transformers import Trainer, AutoModel, AutoTokenizer, TrainingArguments
from torch.utils.data import Dataset

from starwhale import Context, dataset, evaluation, pass_context
from starwhale.api import model, experiment
from starwhale.api.service import api

ROOTDIR = Path(__file__).parent

chatglm = None
tokenizer = None

ds_input_keys = {
    "webqsp": "rawquestion",
    "z_bench_common": "prompt",
    "mkqa": "query", 
}

@evaluation.predict(log_mode="plain")
def ppl(data: dict, external: dict):
    ds_name=external["dataset_uri"].name
    if ds_name in ds_input_keys:
        text = data[ds_input_keys[ds_name]]
    elif "text" in data:
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
    global tokenizer
    if tokenizer is None:
        tokenizer = AutoTokenizer.from_pretrained(
            str(ROOTDIR / "models"), trust_remote_code=True
        )
    global chatglm
    if chatglm is None:
        chatglm = AutoModel.from_pretrained(
            str(ROOTDIR / "models"), trust_remote_code=True
        )
        if os.path.exists(ROOTDIR / "models" / "chatglm-6b-lora.pt"):
            chatglm = load_lora_config(chatglm)
            chatglm.load_state_dict(
                torch.load(ROOTDIR / "models" / "chatglm-6b-lora.pt"), strict=False
            )
    chatglm.half().cuda().eval()
    response, h = chatglm.chat(tokenizer, text, history=[])
    print(f"dataset: {text}\n chatglm6b: {response} \n")
    return response


def load_lora_config(model):
    config = LoraConfig(
        task_type=TaskType.CAUSAL_LM,
        inference_mode=False,
        r=8,
        lora_alpha=32,
        lora_dropout=0.1,
        target_modules=["query_key_value"],
    )
    model = get_peft_model(model, config)
    model.print_trainable_parameters()
    return model


os.environ["CUDA_VISIBLE_DEVICES"] = "0"
device = torch.device("cuda")
max_src_length = 200
max_dst_length = 500

PROMPT_PATTERN = "问：{}"
SEP_PATTERN = "\n答： "


def create_prompt(question):
    return PROMPT_PATTERN.format(question), SEP_PATTERN


def create_prompt_ids(tokenizer, question, max_src_length):
    prompt, sep = create_prompt(question)
    sep_ids = tokenizer.encode(sep, add_special_tokens=True)
    sep_len = len(sep_ids)
    special_tokens_num = 2
    prompt_ids = tokenizer.encode(
        prompt,
        max_length=max_src_length - (sep_len - special_tokens_num),
        truncation=True,
        add_special_tokens=False,
    )

    return prompt_ids + sep_ids


def create_inputs_and_labels(tokenizer, question, answer, device, **kwargs):
    prompt = create_prompt_ids(tokenizer, question, max_src_length)
    completion = tokenizer.encode(
        answer if answer else "",
        max_length=max_dst_length,
        truncation=True,
        add_special_tokens=False,
    )
    eop = tokenizer.eos_token_id
    inputs = prompt + completion + [eop]
    labels = [-100] * len(prompt) + completion + [eop]

    inputs = torch.tensor(inputs, dtype=torch.long, device=device)
    labels = torch.tensor(labels, dtype=torch.long, device=device)
    return inputs, labels


def get_attention_mask(tokenizer, input_ids, device):
    seq = input_ids.tolist()
    bos = tokenizer.bos_token_id
    context_len = seq.index(bos)
    seq_len = len(seq)
    attention_mask = torch.ones((seq_len, seq_len), device=device)
    attention_mask.tril_()
    attention_mask[..., :context_len] = 1
    attention_mask.unsqueeze_(0)
    attention_mask = (attention_mask < 0.5).bool()
    return attention_mask


def get_position_ids(tokenizer, input_ids, device, position_encoding_2d=True):
    seq = input_ids.tolist()
    context_len = seq.index(tokenizer.bos_token_id)
    seq_len = len(seq)
    mask = tokenizer.mask_token_id
    gmask = tokenizer.sp_tokenizer[tokenizer.gmask_token]
    mask_token = mask if mask in seq else gmask
    use_gmask = False if mask in seq else gmask

    mask_position = seq.index(mask_token)

    if position_encoding_2d:
        position_ids = torch.arange(seq_len, dtype=torch.long, device=device)
        if not use_gmask:
            position_ids[context_len:] = mask_position
        block_position_ids = torch.cat(
            (
                torch.zeros(context_len, dtype=torch.long, device=device),
                torch.arange(seq_len - context_len, dtype=torch.long, device=device)
                + 1,
            )
        )
        position_ids = torch.stack((position_ids, block_position_ids), dim=0)
    else:
        position_ids = torch.arange(seq_len, dtype=torch.long, device=device)
        if not use_gmask:
            position_ids[context_len:] = mask_position

    return position_ids


class QADataset(Dataset):
    def __init__(self, sw_dataset, tokenizer) -> None:
        super().__init__()
        self.sw_dataset = sw_dataset
        self.tokenizer = tokenizer

    def __getitem__(self, index):
        data = {"question": "", "answer": ""}
        try:
            item_data = self.sw_dataset[index].features
        except ValueError:
            item_data = {}
        data.update(item_data)
        tokenizer = self.tokenizer
        input_ids, labels = create_inputs_and_labels(tokenizer, device=device, **data)

        attention_mask = get_attention_mask(tokenizer, input_ids, device)
        position_ids = get_position_ids(tokenizer, input_ids, device)

        return {
            "input_ids": input_ids,
            "labels": labels,
            "attention_mask": attention_mask,
            "position_ids": position_ids,
        }

    def __len__(self):
        return len(self.sw_dataset)


def collate_fn(batch):
    input_ids = []
    attention_mask = []
    labels = []
    position_ids = []

    for obj in batch:
        input_ids.append(obj["input_ids"])
        labels.append(obj["labels"])
        attention_mask.append(obj["attention_mask"])
        position_ids.append(obj["position_ids"])

    return {
        "input_ids": torch.stack(input_ids),
        "attention_mask": torch.stack(attention_mask),
        "labels": torch.stack(labels),
        "position_ids": torch.stack(position_ids),
    }


training_args = TrainingArguments(
    "output",
    fp16=True,
    save_steps=500,
    save_total_limit=3,
    gradient_accumulation_steps=1,
    per_device_train_batch_size=1,
    learning_rate=1e-4,
    max_steps=1500,
    logging_steps=50,
    remove_unused_columns=False,
    seed=0,
    data_seed=0,
    group_by_length=False,
    dataloader_pin_memory=False,
)


class ModifiedTrainer(Trainer):
    def compute_loss(self, model, inputs, return_outputs=False):
        return model(
            input_ids=inputs["input_ids"],
            attention_mask=inputs["attention_mask"],
            position_ids=inputs["position_ids"],
            labels=inputs["labels"],
        ).loss


def save_tuned_parameters(model, path):
    saved_params = {
        k: v.to(device) for k, v in model.named_parameters() if v.requires_grad
    }
    torch.save(saved_params, path)


ds_key_selectors = {
    "webqsp": {"rawquestion": "question", "parses[0].Answers[0].EntityName": "answer"},
    "grailqav1": {"answer[0].entity_name": "answer"},
    "graph_questions_testing": {"answer[0]": "answer"},
    "z_bench_common": {"prompt": "question", "gpt4": "answer"},
    "mkqa": {"query": "question", "answers.en[0].text": "answer"},
}


@pass_context
@experiment.fine_tune()
def fine_tune(
    context: Context,
) -> None:
    tokenizer = AutoTokenizer.from_pretrained(
        str(ROOTDIR / "models"), trust_remote_code=True
    )
    chatglm = AutoModel.from_pretrained(str(ROOTDIR / "models"), trust_remote_code=True)
    chatglm = load_lora_config(chatglm)
    if os.path.exists(ROOTDIR / "models" / "chatglm-6b-lora.pt"):
        chatglm.load_state_dict(
            torch.load(ROOTDIR / "models" / "chatglm-6b-lora.pt"), strict=False
        )
    sw_dataset = dataset(context.dataset_uris[0], readonly=True, create="forbid")
    sw_dataset = sw_dataset.with_loader_config(
        field_transformer=ds_key_selectors.get(sw_dataset.name, None)
    )
    train_dataset = QADataset(
        sw_dataset,
        tokenizer=tokenizer,
    )
    trainer = ModifiedTrainer(
        model=chatglm,
        train_dataset=train_dataset,
        args=training_args,
        data_collator=collate_fn,
        tokenizer=tokenizer,
    )
    trainer.train()
    save_tuned_parameters(chatglm, ROOTDIR / "models" / "chatglm-6b-lora.pt")
    model.build(
        workdir=ROOTDIR,
        name="chatglm6b",
        modules=[ppl],
    )


@api(gradio.Text(), gradio.Text())
def online_eval(question: str) -> str:
    return ppl({"text": question})


if __name__ == "__main__":
    Context = namedtuple("Context", ["dataset_uris"])
    context = Context(["webqsp/version/latest"])
    fine_tune(context)
