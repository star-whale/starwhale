from starwhale import dataset

ds = dataset("grailqav1")
row = ds[10]
print(len(ds))
print(row.features)
