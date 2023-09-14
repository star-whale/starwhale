from starwhale import dataset
from starwhale.utils.debug import console

try:
    from .utils import random_text
except ImportError:
    from utils import random_text


def build():
    with dataset("huge-tasks-random-text-1m") as ds:
        console.print(f"start to build dataset: {ds.uri.name}")
        for i in range(0, 1000 * 1000 - 1):
            ds[i] = {
                "label": i,
                "text": random_text(),
            }
            if i % 5000 == 0:
                console.print(f"\t {i} records appended")
        ds.commit()
        console.print(f"generate dataset: {ds.uri}")


if __name__ == "__main__":
    build()
