from starwhale import dataset

ds_name = "ag_news/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
text = row.features["text"]
label = row.features["label"]
print(f"text: {text} \nlabel: {label}")
