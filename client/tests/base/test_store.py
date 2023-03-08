import pathlib
from unittest.mock import patch, MagicMock, PropertyMock

from starwhale.base.blob.store import LocalFileStore


@patch("starwhale.utils.config.load_swcli_config")
@patch(
    "starwhale.utils.config.SWCliConfigMixed.object_store_dir",
    new_callable=PropertyMock,
)
def test_local_file_object_store(
    mock_config: MagicMock, mock_load_conf: MagicMock, tmp_path: pathlib.Path
):
    mock_config.return_value = tmp_path
    store = LocalFileStore()
    assert store.root == tmp_path

    file = store.get("not_exist")
    assert file is None

    file = store.put(__file__)
    assert file is not None
    assert file.hash

    again = store.get(file.hash)
    assert again.hash == file.hash
    assert again.path == store.root / file.hash[:2] / file.hash

    store.copy_dir(pathlib.Path(__file__).parent, tmp_path / "test")
    dst = tmp_path / "test" / "test_store.py"
    assert dst.exists()
    assert dst.stat().st_ino == file.path.stat().st_ino
