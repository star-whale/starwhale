import typing as t
from pathlib import Path

from starwhale import Video, MIMEType, BuildExecutor

root_dir = Path(__file__).parent.parent
dataset_dir = root_dir / "data" / "UCF-101"
test_ds_path = [root_dir / "data" / "test_list.txt"]


class UCFDatasetBuildExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple, None, None]:
        for path in test_ds_path:
            with path.open() as f:
                for line in f.readlines():
                    v_id, label, video_sub_path = line.split()

                    data_path = dataset_dir / video_sub_path
                    data = Video(
                        data_path,
                        display_name=video_sub_path,
                        shape=(1,),
                        mime_type=MIMEType.WEBM,
                    )

                    yield f"{label}_{video_sub_path}", {
                        "video": data,
                        "label": label,
                    }
