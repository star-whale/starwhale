from starwhale import dataset

ds = dataset("mkqa")
row = ds[10]
print(row.features)
