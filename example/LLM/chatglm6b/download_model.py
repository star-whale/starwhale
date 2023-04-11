from transformers import AutoModel, AutoTokenizer

tokenizer = AutoTokenizer.from_pretrained("THUDM/chatglm-6b", trust_remote_code=True)
model = (
    AutoModel.from_pretrained("THUDM/chatglm-6b", trust_remote_code=True).half().cuda()
)
tokenizer.save_pretrained("./models")
model.save_pretrained("./models")
