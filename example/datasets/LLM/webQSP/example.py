from starwhale import dataset

ds = dataset("webqsp")
row = ds[10]
print(row.features)
