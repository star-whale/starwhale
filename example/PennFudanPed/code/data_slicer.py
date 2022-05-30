import io
from pathlib import Path
from PIL import Image
from starwhale.api.dataset import BuildExecutor


class PennFudanPedSlicer(BuildExecutor):

    idx_
    def iter_data_slice(self, path: str):
        img = Image.open(path).convert("RGB")
        img_byte_arr = io.BytesIO()
        img.save(img_byte_arr, format='PNG')
        img_byte_arr = img_byte_arr.getvalue()
        return [img_byte_arr]

    def iter_label_slice(self, path: str):
        img = Image.open(path)
        img_byte_arr = io.BytesIO()
        img.save(img_byte_arr, format='PNG')
        return [img_byte_arr.getvalue()]


if __name__ == "__main__":
    executor = PennFudanPedSlicer(
        data_dir=Path("../data"),
        data_filter="PNGImages/*6.png", label_filter="PedMasks/*6_mask.png",
        batch=1,
        alignment_bytes_size=4 * 1024,
        volume_bytes_size=4 * 1024 * 1024,
    )
    executor.make_swds()
