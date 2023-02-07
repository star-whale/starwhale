from transformers import AutoTokenizer, AutoModelForSequenceClassification

tokenizer = AutoTokenizer.from_pretrained("textattack/albert-base-v2-ag-news")
model = AutoModelForSequenceClassification.from_pretrained(
    "textattack/albert-base-v2-ag-news"
)
tokenizer.save_pretrained("./ag_news/models")
model.save_pretrained("./ag_news/models")
