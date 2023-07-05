# pip install -q transformers accelerate starwhale
import os
from typing import Any, List, Tuple
from pathlib import Path

import numpy as np
import torch
from datasets import Dataset
from transformers import (
    AdamW,
    AutoModel,
    AutoTokenizer,
    AutoModelForSeq2SeqLM,
    get_linear_schedule_with_warmup,
)

from starwhale import Context, dataset, handler, fine_tune, evaluation, pass_context
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

        download()

    global tokenizer
    if tokenizer is None:
        tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    global model
    if model is None:
        model = AutoModelForSeq2SeqLM.from_pretrained(
            checkpoint, torch_dtype="auto", device_map="auto"
        )
        model.eval()

    ds_name = external["dataset_uri"].name
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
    return tokenizer.decode(outputs[0])


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


@pass_context
@fine_tune()
def ft(context: Context) -> None:
    ft_inner(context=context)


def ft_inner(
    context: Context = None,
    swds: str = "mkqa/version/latest",
) -> None:
    checkpoint = str(ROOTDIR / "models")
    if not os.path.exists(checkpoint):
        from download_model import download

        download()

    tokeniser = AutoTokenizer.from_pretrained(
        str(ROOTDIR / "models"), trust_remote_code=True
    )
    model = AutoModelForSeq2SeqLM.from_pretrained(
        checkpoint, torch_dtype="auto", device_map="auto"
    )
    max_length = 100
    swds_name = context.dataset_uris[0] if context else swds
    sw_dataset = dataset(swds_name, readonly=True, create="forbid")
    sw_dataset = sw_dataset.with_loader_config(
        field_transformer=ds_key_selectors.get(sw_dataset._uri.name, None)
    )
    hgds = swds2hgds(sw_dataset)
    hgds = (
        hgds.shuffle()
        .map(
            lambda elem: {
                "input_ids": tokeniser.encode(
                    elem.get("instruction", "") or "",
                    padding="max_length",
                    truncation=True,
                    max_length=max_length,
                ),
                "labels": tokeniser.encode(
                    elem.get("output", "") or "",
                    padding="max_length",
                    truncation=True,
                    max_length=max_length,
                ),
                # "label": elem["output"],
            }
        )
        .train_test_split(test_size=0.1)
    )
    batch_size = os.getenv("MT0_TRAIN_BATCH_SIZE") or 16

    hgds = hgds["train"]

    def ds_gen():
        current_item = 0
        while True:
            start = current_item
            current_item += batch_size
            if current_item >= len(hgds):
                break
            datas = hgds[start:current_item]
            yield torch.tensor(datas["input_ids"]).cuda(), torch.tensor(
                datas["labels"]
            ).cuda()

    n_epochs = int(os.getenv("MT0_TRAIN_EPOCHS")) or 8
    print(f"epochs is {n_epochs}")
    # batch_size = 16
    print_freq = 50
    lr = 5e-4
    n_batches = int(np.ceil(len(hgds) / batch_size))
    total_steps = n_epochs * n_batches
    n_warmup_steps = int(total_steps * 0.01)
    # Optimizer
    optimizer = AdamW(model.parameters(), lr=lr)
    scheduler = get_linear_schedule_with_warmup(optimizer, n_warmup_steps, total_steps)
    losses = []

    for epoch_idx in range(n_epochs):
        # Randomize data order

        for batch_idx, (input_batch, label_batch) in enumerate(ds_gen()):
            optimizer.zero_grad()

            # Forward pass
            model_out = model.forward(input_ids=input_batch, labels=label_batch)

            # Calculate loss and update weights
            loss = model_out.loss
            losses.append(loss.item())
            loss.backward()
            optimizer.step()
            scheduler.step()

            # Print training update info
            if (batch_idx + 1) % print_freq == 0:
                avg_loss = np.mean(losses[-print_freq:])
                print(
                    "Epoch: {} | Step: {} | Avg. loss: {:.3f} | lr: {}".format(
                        epoch_idx + 1,
                        batch_idx + 1,
                        avg_loss,
                        scheduler.get_last_lr()[0],
                    )
                )

    torch.save(model.state_dict(), str(ROOTDIR / "models" / "pytorch_model.bin"))
    swmp.build(
        workdir=ROOTDIR,
        name="mt0",
        modules=[ft, ppl],
    )


def swds2hgds(swds) -> Any:
    sw_ds = swds

    def my_gen():
        for item in sw_ds:
            yield item.features

    return Dataset.from_generator(my_gen)


@handler(expose=7860)
@torch.no_grad()
def chatbot():
    import gradio as gr

    # os.environ['CUDA_VISIBLE_DEVICES'] ='0'
    checkpoint = ROOTDIR / "models"
    if not os.path.exists(checkpoint):
        import download_model  # noqa: F401
    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    mt0 = AutoModelForSeq2SeqLM.from_pretrained(
        checkpoint, torch_dtype="auto", device_map="auto"
    )
    mt0.eval()

    def gen_prompt(message, chat_histroy):
        if not chat_histroy:
            return message
        prompt = ""
        for i, (q, r) in enumerate(chat_histroy):
            prompt += "[Round {}]\answer the chat below:{}\nanswer:{}\n".format(i, q, r)
        prompt += "[Round {}]\answer the chat below:{}\nanswer:".format(
            len(chat_histroy), message
        )
        return prompt

    with gr.Blocks() as demo:
        chatbot = gr.Chatbot()
        msg = gr.Textbox()
        clear = gr.ClearButton([msg, chatbot])
        max_length = gr.Slider(
            0, 4096, value=2048, step=1.0, label="Maximum length", interactive=True
        )
        top_p = gr.Slider(0, 1, value=0.7, step=0.01, label="Top P", interactive=True)
        temperature = gr.Slider(
            0, 1, value=0.95, step=0.01, label="Temperature", interactive=True
        )

        def respond(message, chat_history, mxl, tpp, tmp):
            gen_kwargs = {
                "max_length": mxl,
                "num_beams": 1,
                "do_sample": True,
                "top_p": tpp,
                "temperature": tmp,
            }
            ct = chat_history[-5] if len(chat_history) > 5 else chat_history
            inputs = tokenizer.encode(gen_prompt(message, ct), return_tensors="pt")
            outputs = mt0.generate(inputs, **gen_kwargs)
            response = tokenizer.decode(outputs[0])
            response = response.replace(tokenizer.pad_token, "")
            response = response.replace(tokenizer.eos_token, "")
            chat_history.append((message, response))
            return "", chat_history

        msg.submit(
            respond, [msg, chatbot, max_length, top_p, temperature], [msg, chatbot]
        )

    demo.launch(server_name="0.0.0.0")


if __name__ == "__main__":
    chatbot()
