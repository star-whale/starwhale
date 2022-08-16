import io
import pickle
import typing as t

from PIL import Image

from starwhale.api.dataset import BuildExecutor


class FileBytes:
    def __init__(self, p, byte_array):
        self.file_path = p
        self.content_bytes = byte_array


def _pickle_data(image_file_paths):
    all_bytes = [
        FileBytes(image_f, _image_to_bytes(image_f)) for image_f in image_file_paths
    ]
    return pickle.dumps(all_bytes)


def _pickle_label(label_file_paths):
    all_bytes = [
        FileBytes(label_f, _label_to_bytes(label_f)) for label_f in label_file_paths
    ]
    return pickle.dumps(all_bytes)


def _label_to_bytes(label_file_path):
    img = Image.open(label_file_path)
    img_byte_arr = io.BytesIO()
    img.save(img_byte_arr, format="PNG")
    return img_byte_arr.getvalue()


def _image_to_bytes(image_file_path):
    img = Image.open(image_file_path).convert("RGB")
    img_byte_arr = io.BytesIO()
    img.save(img_byte_arr, format="PNG")
    return img_byte_arr.getvalue()


class PennFudanPedSlicer(BuildExecutor):
    def iter_data_slice(self, path: str):
        pass

    def iter_label_slice(self, path: str):
        pass

    def iter_all_dataset_slice(self) -> t.Generator[t.Any, None, None]:
        datafiles = [p for p in self.iter_data_files()]
        idx = 0
        data_size = len(datafiles)
        while True:
            last_idx = idx
            idx += 1
            if idx > data_size:
                break
            yield _pickle_data(datafiles[last_idx:idx])

    def iter_all_label_slice(self) -> t.Generator[t.Any, None, None]:
        labelfiles = [p for p in self.iter_label_files()]
        idx = 0
        data_size = len(labelfiles)
        while True:
            last_idx = idx
            idx += 1
            if idx > data_size:
                break
            yield _pickle_label(labelfiles[last_idx:idx])

    def iter_data_slice(self, path: str):
        pass

    def iter_label_slice(self, path: str):
        pass
