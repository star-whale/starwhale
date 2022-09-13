import typing as t
from pathlib import Path

from starwhale.api.dataset import Audio, MIMEType, BuildExecutor

dataset_dir = (
    Path(__file__).parent.parent / "data" / "SpeechCommands" / "speech_commands_v0.02"
)
validation_ds_paths = [dataset_dir / "validation_list.txt"]
testing_ds_paths = [dataset_dir / "testing_list.txt"]


class SpeechCommandsBuildExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:

        idx = 1

        for path in validation_ds_paths:
            with path.open() as f:
                for item in f.readlines():
                    item = item.strip()
                    if not item:
                        continue

                    if idx > 100:
                        break

                    idx += 1

                    data_path = dataset_dir / item
                    data = Audio(
                        data_path, display_name=item, shape=(1,), mime_type=MIMEType.WAV
                    )

                    speaker_id, utterance_num = data_path.stem.split("_nohash_")
                    annotations = {
                        "label": data_path.parent.name,
                        "speaker_id": speaker_id,
                        "utterance_num": int(utterance_num),
                    }
                    yield data, annotations
