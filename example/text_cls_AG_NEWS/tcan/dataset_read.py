from starwhale import dataset

ds_name = "ag_news/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
text = row.data["text"]
label = row.data["label"]
print(f"text: {text} \nlabel: {label}")
