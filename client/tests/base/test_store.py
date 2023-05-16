import shutil
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

    dst = tmp_path / "test"
    store.copy_dir(pathlib.Path(__file__).parent, dst, [])
    script = tmp_path / "test" / "test_store.py"
    assert script.exists()
    assert script.stat().st_ino == file.path.stat().st_ino

    # copy with exclude
    for i, exclude in enumerate(["*.py", "test_store.py", "/*"]):
        store.copy_dir(pathlib.Path(__file__).parent, tmp_path / f"test-{i}", [exclude])
        assert not (tmp_path / f"test-{i}" / "test_store.py").exists()

    # copy with venv in the working dir
    workdir = tmp_path / "workdir"
    venv = workdir / "venv"
    # simulate a venv in the working dir
    venv.mkdir(parents=True, exist_ok=True)
    (venv / "pyvenv.cfg").touch()
    # simulate a conda env in the working dir
    conda = workdir / "conda"
    conda.mkdir(parents=True, exist_ok=True)
    (conda / "conda-meta").mkdir(parents=True, exist_ok=True)
    # make a fake file in the conda meta dir, make sure the copy touches the dir
    (conda / "conda-meta" / "fake").touch()

    # user script
    (workdir / "main.py").touch()

    env_files = {
        dst / "venv",
        dst / "venv" / "pyvenv.cfg",
        dst / "conda",
        dst / "conda" / "conda-meta",
        dst / "conda" / "conda-meta" / "fake",
    }

    shutil.rmtree(dst)
    # do not ignore env paths
    sz = store.copy_dir(workdir, dst, [], ignore_venv_or_conda=False)
    assert (dst / "main.py").exists()
    assert all(f.exists() for f in env_files)
    assert sz == (dst / "main.py").stat().st_size + sum(
        f.stat().st_size for f in [venv / "pyvenv.cfg", conda / "conda-meta" / "fake"]
    )

    shutil.rmtree(dst)
    # ignore env paths
    sz = store.copy_dir(workdir, dst, [], ignore_venv_or_conda=True)
    assert (dst / "main.py").exists()
    assert not any(f.exists() for f in env_files)
    assert sz == (dst / "main.py").stat().st_size
    ignored = (dst / "venv", dst / "conda" / "conda-meta")
    assert all(not f.exists() for f in ignored)

    shutil.rmtree(dst)
    # ignore env paths with exclude
    sz = store.copy_dir(workdir, dst, ["venv/*", "conda/*"], ignore_venv_or_conda=False)
    assert (dst / "main.py").exists()
    assert not any(f.exists() for f in env_files)
    assert sz == (dst / "main.py").stat().st_size
    assert dst / "venv"
    ignored = (dst / "venv" / "pyvenv.cfg", dst / "conda" / "conda-meta" / "fake")
    assert all(not f.exists() for f in ignored)
