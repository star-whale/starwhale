from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils.fs import copy_file, ensure_dir
from starwhale.utils.error import FormatError, NotFoundError


class FsUtilsTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_abnormal_copy_file(self) -> None:
        with self.assertRaises(NotFoundError):
            copy_file(Path("not_found"), Path("/tmp/target/not_found"))

        src = Path("/tmp/src/dir")
        dest = Path("/tmp/dest/dummy")
        assert not src.exists()
        assert not dest.exists()

        ensure_dir(src)
        assert src.exists()
        assert src.is_dir()

        with self.assertRaises(FormatError):
            copy_file(src, dest)

    def test_copy_file(self) -> None:
        src_dir = Path("/tmp/src")
        assert not src_dir.exists()

        ensure_dir(src_dir)
        src_file = src_dir / "file"
        assert not src_file.exists()

        contents = "helloworld"
        self.fs.create_file(src_file, contents=contents)
        assert src_file.exists() and src_file.is_file()

        dest_file = Path("/tmp/dest/file")
        assert not dest_file.exists()

        copy_file(src_file, dest_file)
        assert dest_file.exists() and dest_file.is_file()
        assert contents == dest_file.read_text()
