from starwhale import Link, Text, Image, Binary, dataset  # noqa: F401

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/kitti/object_birds/training"
image2_formatter = "/image_2/{num}.png"
image3_formatter = "/image_3/{num}.png"
label_formatter = "/label_2/{num}.txt"
velodyne_formatter = "/velodyne/{num}.bin"


def build_ds():
    ds = dataset("kitti-object-birds")
    for i in range(7481):
        num = str(i).zfill(6)
        ds.append(
            {
                "image_2": Image(
                    link=Link((PATH_ROOT + image2_formatter).format(num=num))
                ),
                "image_3": Image(
                    link=Link((PATH_ROOT + image3_formatter).format(num=num))
                ),
                "label_2": Text(
                    link=Link((PATH_ROOT + label_formatter).format(num=num))
                ),
                "velodyne": Binary(
                    link=Link((PATH_ROOT + velodyne_formatter).format(num=num))
                ),
            }
        )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
