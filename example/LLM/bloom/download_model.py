import sys
from pathlib import Path

from transformers import AutoTokenizer, AutoModelForSeq2SeqLM


def download(checkpoint: str = "bigscience/mt0-xxl"):
    ROOTDIR = Path(__file__).parent

    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    model = AutoModelForSeq2SeqLM.from_pretrained(
        checkpoint, torch_dtype="auto", device_map="auto"
    )
    print(str(ROOTDIR / "models"))
    tokenizer.save_pretrained(str(ROOTDIR / "models"))
    model.save_pretrained(str(ROOTDIR / "models"))
    del model
    del tokenizer


if __name__ == "__main__":
    if sys.argv[1]:
        download(sys.argv[1])
    else:
        download()
