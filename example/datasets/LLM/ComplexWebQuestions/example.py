from starwhale import dataset

ds = dataset("compwebq")
row = ds[10]
print(row.features)
