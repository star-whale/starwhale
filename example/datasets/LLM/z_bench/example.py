import sys

from starwhale import dataset

ds_name = sys.argv[1]
ds = dataset(f"z_bench_{ds_name}/version/latest")
row = ds[10]
print(row.data)
