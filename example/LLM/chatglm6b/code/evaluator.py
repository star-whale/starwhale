import sys
from pathlib import Path

from transformers import AutoModel, AutoTokenizer

from starwhale import evaluation

ROOTDIR = Path(__file__).parent.parent
tokenizer = AutoTokenizer.from_pretrained(
    str(ROOTDIR / "models"), trust_remote_code=True
)
model = (
    AutoModel.from_pretrained(str(ROOTDIR / "models"), trust_remote_code=True)
    .half()
    .cuda()
)
model = model.eval()


@evaluation.predict
def ppl(data: dict, **kw):
    text = data["text"]
    response, _ = model.chat(tokenizer, text, history=[])
    print(f"dataset: {text}\n chatglm6b: {response} \n")
    return response


if __name__ == "__main__":
    ppl({"text": sys.argv[1]})
