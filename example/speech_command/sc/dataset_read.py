from starwhale import dataset

ds_name = "speech_command/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
speech = row.data["speech"]
command = row.data["command"]
print(command)
