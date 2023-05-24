from pathlib import Path

from transformers import AutoModel, AutoTokenizer

ROOTDIR = Path(__file__).parent
print(str(ROOTDIR / "models"))
tokenizer = AutoTokenizer.from_pretrained("THUDM/chatglm-6b", trust_remote_code=True)
model = (
    AutoModel.from_pretrained("THUDM/chatglm-6b", trust_remote_code=True).half().cuda()
)
print(str(ROOTDIR / "models"))
tokenizer.save_pretrained(str(ROOTDIR / "models"))
model.save_pretrained(str(ROOTDIR / "models"))
del model
del tokenizer
