from starwhale import dataset

ds = dataset("qald9")
row = ds[10]
print(row.features)
