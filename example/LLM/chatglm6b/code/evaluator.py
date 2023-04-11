from pathlib import Path

from transformers import AutoTokenizer, AutoModel

ROOTDIR = Path(__file__).parent.parent
_LABEL_NAMES = ["LABEL_0", "LABEL_1", "LABEL_2", "LABEL_3"]

tokenizer = AutoTokenizer.from_pretrained(ROOTDIR/"models", trust_remote_code=True)
model = AutoModel.from_pretrained(ROOTDIR/"models", trust_remote_code=True).half().cuda()
model = model.eval()
response, history = model.chat(tokenizer, "你好", history=[])
print(response)
response, history = model.chat(tokenizer, "晚上睡不着应该怎么办", history=history)
print(response)

