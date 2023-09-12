import dataset

from starwhale import dataset as sw_ds


def build_ds():
    ds = sw_ds("coco")
    for d in dataset.do_iter_item_from_http():
        ds.append(d)
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
