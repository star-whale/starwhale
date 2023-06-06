from pathlib import Path

from transformers import BloomTokenizerFast, BloomForCausalLM

ROOTDIR = Path(__file__).parent

def download(checkpoint :str = "bigscience/bloom-560m"):
    tokenizer = BloomTokenizerFast.from_pretrained(
        f"{checkpoint}", add_prefix_space=True
    )
    model = BloomForCausalLM.from_pretrained(f"{checkpoint}")
    print(str(ROOTDIR / "models"))
    tokenizer.save_pretrained(str(ROOTDIR / "models"))
    model.save_pretrained(str(ROOTDIR / "models"))
    del model
    del tokenizer


if __name__ == "__main__":
    download()
