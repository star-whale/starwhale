from starwhale import dataset

ds = dataset("kqapro")
row = ds[10]
print(row.features)
