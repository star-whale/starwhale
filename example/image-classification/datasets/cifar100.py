from __future__ import annotations

import io
import pickle
from pathlib import Path

from PIL import Image as PILImage
from tqdm import tqdm

from starwhale import Image, dataset, MIMEType

DATASET_NAME = "cifar100"
ROOT_DIR = Path(__file__).parent
DATA_DIR = ROOT_DIR / "data" / "cifar-100-python"


def build():
    # If the dataset already exists, it will raise an exception. We assure to build the dataset only once.
    with dataset(DATASET_NAME, create="empty") as ds:
        with (DATA_DIR / "meta").open("rb") as f:
            meta = pickle.load(f, encoding="bytes")

        with (DATA_DIR / "train").open("rb") as f:
            content = pickle.load(f, encoding="bytes")
            for data, fine_label, coarse_label, filename in zip(
                content[b"data"],
                content[b"fine_labels"],
                content[b"coarse_labels"],
                content[b"filenames"],
            ):
                image_array = data.reshape(3, 32, 32).transpose(1, 2, 0)
                image_bytes = io.BytesIO()
                PILImage.fromarray(image_array).save(image_bytes, format="PNG")

                # Starwhale dataset will use incremental id for each item automatically.
                ds.append(
                    {
                        "fine_label": fine_label,
                        "fine_label_name": meta[b"fine_label_names"][
                            fine_label
                        ].decode(),
                        "coarse_label": coarse_label,
                        "coarse_label_name": meta[b"coarse_label_names"][
                            coarse_label
                        ].decode(),
                        "image": Image(
                            fp=image_bytes.getvalue(),
                            display_name=filename.decode(),
                            shape=image_array.shape,
                            mime_type=MIMEType.PNG,
                        ),
                    }
                )

        # commit will generate a new version of the dataset.
        ds.commit()


def show():
    # the dataset must exist.
    ds = dataset(DATASET_NAME, readonly=True)
    # head will return a generator of the first n items.
    for row in ds.head(5):
        with PILImage.open(io.BytesIO(row.features.image.to_bytes())) as img:
            img.show()
        print(
            f"other data: "
            f"fine({row.features['coarse_label_name']}[{row.features['coarse_label']}]), "
            f"coarse({row.features['fine_label_name']}[{row.features['fine_label']}])"
        )
    ds.close()


if __name__ == "__main__":
    build()
    show()
