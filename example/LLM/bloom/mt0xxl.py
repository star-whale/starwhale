# pip install -q transformers accelerate starwhale
import os
from pathlib import Path

from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

from starwhale import evaluation

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
        import download_model  # noqa: F401
    global tokenizer
    if tokenizer is None:
        tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    global model
    if model is None:
        model = AutoModelForSeq2SeqLM.from_pretrained(
            checkpoint, torch_dtype="auto", device_map="auto"
        )

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
