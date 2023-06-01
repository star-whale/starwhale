from pathlib import Path

from transformers import AutoTokenizer, AutoModelForSeq2SeqLM


def download():
    ROOTDIR = Path(__file__).parent
    checkpoint = "bigscience/mt0-xxl"

    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    model = AutoModelForSeq2SeqLM.from_pretrained(
        checkpoint, torch_dtype="auto", device_map="auto"
    )
    print(str(ROOTDIR / "models"))
    tokenizer.save_pretrained(str(ROOTDIR / "models"))
    model.save_pretrained(str(ROOTDIR / "models"))
    del model
    del tokenizer


if __name__ == "main":
    download()
