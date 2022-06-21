import os
import typing as t
import pickle
from pathlib import Path
from starwhale.api.dataset import BuildExecutor


class FileBytes:
    def __init__(self, p):
        self.file_path = p
        self.content_bytes = open(p, "rb").read()


def _pickle_data(audio_file_paths):
    all_bytes = [FileBytes(audio_f) for audio_f in audio_file_paths]
    return pickle.dumps(all_bytes)


def _pickle_label(audio_file_paths):
    all_strings = [os.path.basename(os.path.dirname(str(audio_f))) for audio_f
                   in audio_file_paths]
    return pickle.dumps(all_strings)


class SpeechCommandsSlicer(BuildExecutor):

    def load_list(self, file_filter):
        filepath = self.data_dir / file_filter
        with open(filepath) as fileobj:
            return [self.data_dir / line.strip() for line in fileobj]

    def _iter_files(
        self, file_filter: str, sort_key: t.Optional[t.Any] = None
    ) -> t.Generator[Path, None, None]:
        _key = sort_key
        if _key is not None and not callable(_key):
            raise Exception(f"data_sort_func({_key}) is not callable.")

        _files = sorted(self.load_list(file_filter), key=_key)
        for p in _files:
            if not p.is_file():
                continue
            yield p

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
            idx = idx + self._batch
            if idx > data_size:
                break
            yield _pickle_data(datafiles[last_idx:idx])

    def iter_all_label_slice(self) -> t.Generator[t.Any, None, None]:
        datafiles = [p for p in self.iter_data_files()]
        idx = 0
        data_size = len(datafiles)
        while True:
            last_idx = idx
            idx = idx + self._batch
            if idx > data_size:
                break
            yield _pickle_label(datafiles[last_idx:idx])
