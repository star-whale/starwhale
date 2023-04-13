from datasets import load_dataset

from starwhale import dataset

hg_ds = load_dataset("mkqa", split="train")
sw_ds = dataset("mkqa")
for item in enumerate(hg_ds):
    sw_ds.append(item[1])
sw_ds.commit()
sw_ds.close()
