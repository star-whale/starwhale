import typing as t
from pathlib import Path

from starwhale import Link, Audio, MIMEType, S3LinkAuth

dataset_dir = (
    Path(__file__).parent.parent / "data" / "SpeechCommands" / "speech_commands_v0.02"
)
validation_ds_paths = [dataset_dir / "validation_list.txt"]
testing_ds_paths = [dataset_dir / "testing_list.txt"]


class SWDSBuildExecutor:
    def __iter__(self) -> t.Generator[t.Tuple, None, None]:
        for path in validation_ds_paths:
            with path.open() as f:
                for item in f.readlines():
                    item = item.strip()
                    if not item:
                        continue

                    data_path = dataset_dir / item
                    data = Audio(
                        data_path, display_name=item, shape=(1,), mime_type=MIMEType.WAV
                    )

                    speaker_id, utterance_num = data_path.stem.split("_nohash_")
                    yield item, {
                        "speech": data,
                        "command": data_path.parent.name,
                        "speaker_id": speaker_id,
                        "utterance_num": int(utterance_num),
                    }


class LinkRawDatasetBuildExecutor:

    _auth = S3LinkAuth(name="speech", access_key="minioadmin", secret="minioadmin")
    _addr = "10.131.0.1:9000"
    _bucket = "users"

    def __iter__(self) -> t.Generator[t.Tuple, None, None]:
        import boto3
        from botocore.client import Config

        s3 = boto3.resource(
            "s3",
            endpoint_url=f"http://{self._addr}",
            aws_access_key_id=self._auth.access_key,
            aws_secret_access_key=self._auth.secret,
            config=Config(signature_version="s3v4"),
            region_name=self._auth.region,
        )

        objects = s3.Bucket(self._bucket).objects.filter(
            Prefix="dataset/SpeechCommands/speech_commands_v0.02"
        )
        for obj in objects:
            path = Path(obj.key)  # type: ignore
            command = path.parent.name
            if (
                command == "_background_noise_"
                or "_nohash_" not in path.name
                or obj.size < 10240
                or not path.name.endswith(".wav")
            ):
                continue
            speaker_id, utterance_num = path.stem.split("_nohash_")
            uri = f"s3://{self._addr}/{self._bucket}/{obj.key.lstrip('/')}"
            idx = f"{command}/{path.name}"
            data = Audio(
                mime_type=MIMEType.WAV,
                shape=(1,),
                link=Link(
                    uri,
                ),
            )
            yield idx, {
                "speech": data,
                "command": command,
                "speaker_id": speaker_id,
                "utterance_num": int(utterance_num),
            }
