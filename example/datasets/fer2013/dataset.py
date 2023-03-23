import requests

from starwhale import dataset, GrayscaleImage  # noqa: F401
from starwhale.utils.retry import http_retry

DATA_PATH = (
    "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/fer2013/train.csv"
)


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("fer2013")
    lines = request_link_text(DATA_PATH).splitlines()
    for line in lines:
        tokens = line.split(",")
        if len(tokens) != 2:
            continue
        if not tokens[0].isnumeric():
            continue
        label = tokens[0]
        data_str = tokens[1]
        _data = bytes([int(t) for t in data_str.strip('"').split()])
        ds.append(
            (
                {
                    "image": GrayscaleImage(
                        _data,
                        display_name=label,
                        shape=(48, 48),
                    ),
                    "label": label,
                },
            )
        )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
