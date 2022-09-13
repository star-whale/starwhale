import typing as t
from pathlib import Path

from starwhale.api.dataset import Audio, MIMEType, BuildExecutor


class SpeechCommandsSlicer(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        dataset_dir = (
            Path(__file__).parent.parent
            / "data"
            / "SpeechCommands"
            / "speech_commands_v0.02"
        )

        with (dataset_dir / "testing_list.txt").open() as f:
            for item in f.readlines():
                item = item.strip()
                if not item:
                    continue

                data_path = dataset_dir / item
                data = Audio(
                    data_path, display_name=item, shape=(1,), mime_type=MIMEType.WAV
                )
                annotations = {"label": data_path.parent.name}
                yield data, annotations
