from starwhale import dataset
from starwhale.utils.debug import console

try:
    from .utils import random_text
except ImportError:
    from utils import random_text

cnt = 200 * 1000 - 1


def iter_items():
    for i in range(0, cnt):
        yield {
            "label": i,
            "text": random_text(),
        }


if __name__ == "__main__":
    with dataset("huge-tasks-random-text") as ds:
        console.print(f"start to build dataset: {ds.uri.name}")
        for item in iter_items():
            ds.append(item)
        ds.commit()
        console.print(f"generate dataset: {ds.uri}")
