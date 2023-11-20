import io
from pathlib import Path

import numpy as np
import requests
from PIL import Image as PILImage
from tqdm import tqdm

from starwhale import Image, dataset, MIMEType, init_logger

init_logger(3)


def download(url, to_path):
    if to_path.exists():
        print(f"skip download {url}, file {to_path} already exists")
        return

    to_path.parent.mkdir(parents=True, exist_ok=True)

    with requests.get(url, timeout=60, stream=True) as r:
        r.raise_for_status()
        size = int(r.headers.get("content-length", 0))
        with tqdm(
            total=size,
            unit="B",
            unit_scale=True,
            initial=0,
            unit_divisor=1024,
        ) as bar:
            with to_path.open("wb") as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)
                    bar.update(len(chunk))


def build_dataset():
    data_dir = Path(__file__).parent / "data"
    # This is a copy from scikit-learn lib: https://github.com/scikit-learn/scikit-learn/blob/main/sklearn/datasets/data/digits.csv.gz
    fname = "digits.csv"
    url = (
        f"https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/mnist/{fname}"
    )

    download(url, data_dir / fname)

    with dataset("mnist64") as ds:
        with (data_dir / fname).open("r") as f:
            data = np.loadtxt(f, delimiter=",")
            targets = data[:, -1].astype(int, copy=False)
            images = data[:, :-1]

            for i in tqdm(range(0, min(500, len(images)))):
                img_2d_array = (np.reshape(images[i], (8, 8)) * 255).astype(np.uint8)
                img_bytes = io.BytesIO()
                PILImage.fromarray(img_2d_array, "L").save(img_bytes, format="PNG")
                ds[i] = {
                    "img": Image(
                        img_bytes.getvalue(),
                        display_name=f"{i}",
                        shape=(8, 8, 1),
                        mime_type=MIMEType.PNG,
                    ),
                    "label": targets[i],
                }
        ds.commit()

    print(f"{ds.uri} dataset build done")


if __name__ == "__main__":
    build_dataset()
