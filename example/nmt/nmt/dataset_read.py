from starwhale import dataset

ds_name = "nmt/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
english = row.data["english"]
french = row.data["french"]
print(f"english: {english.content} \n french: {french.content}")
