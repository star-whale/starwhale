from starwhale import dataset

ds_name = "ucf101/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
video = row.features["video"]
label = row.features["label"]
print(label)
