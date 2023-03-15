from starwhale import dataset

ds_name = "nmt/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
english = row.features["english"]
french = row.features["french"]
print(f"english: {english.content} \n french: {french.content}")
