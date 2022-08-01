import os
import sys
import tempfile
from pathlib import Path
from unittest import TestCase
from unittest.mock import patch

from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.core.runtime.process import Process


class _SimpleTask:
    def __init__(self, name: str, dest_dir: Path) -> None:
        self.name = name
        self.dest_dir = dest_dir

    def run(self, id: int, verbose: bool = False) -> None:
        msg = f"start to run simpleTask:{self.name}, id:{id}..."
        print(msg)
        ensure_dir(self.dest_dir)
        ensure_file(self.dest_dir / self.name, content=f"{id}")
        assert os.environ.get(Process.EnvInActivatedProcess) == "1"

        if verbose:
            ensure_file(
                self.dest_dir / f"{self.name}.verbose", content=f"verbose info: {msg}"
            )


class RuntimeProcessTestCase(TestCase):
    def setUp(self) -> None:
        self.root = Path(tempfile.mkdtemp(prefix="starwhale-ut-"))
        ensure_dir(self.root)

    def tearDown(self) -> None:
        empty_dir(self.root)

    def test_venv_prefix_patch(self) -> None:
        prefix_path = self.root / "venv"
        ensure_dir(prefix_path)
        ensure_file(prefix_path / "pyvenv.cfg", content="venv")
        pybin = f"{prefix_path}/bin/python3"

        patcher = patch("starwhale.core.runtime.process.check_call")
        m_call = patcher.start()

        st = _SimpleTask(name="venv", dest_dir=self.root / "task")
        p = Process(prefix_path=prefix_path, target=st.run, args=(1,))
        p.run()

        assert m_call.call_args[0][0][0:2] == [
            "bash",
            "-c",
        ]
        assert m_call.call_args[0][0][2].startswith(
            f"source {prefix_path}/bin/activate && {prefix_path}/bin/python3 -c"
        )
        patcher.stop()

        ensure_dir(prefix_path / "bin")
        ensure_file(prefix_path / "bin" / "activate", content="export SW_ACTIVATE=1")

        os.symlink(sys.executable, str(pybin))

        st = _SimpleTask(name="venv", dest_dir=self.root / "task")
        p = Process(prefix_path=prefix_path, target=st.run, args=(1,))
        p.run()

        assert (self.root / "task" / "venv").exists()
        assert not (self.root / "task" / "venv.verbose").exists()
        assert (self.root / "task" / "venv").read_text() == "1"

        st = _SimpleTask(name="venv", dest_dir=self.root / "task")
        p = Process(
            prefix_path=prefix_path, target=st.run, args=(1,), kwargs={"verbose": True}
        )
        p.run()
        assert (self.root / "task" / "venv.verbose").exists()
