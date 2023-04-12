from starwhale import dataset

ds = dataset("graph_questions_testing")
row = ds[10]
print(row.features)
