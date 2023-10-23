from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils.fs import (
    copy_file,
    file_stat,
    ensure_dir,
    ensure_file,
    extract_tar,
    is_within_dir,
    cmp_file_content,
)
from starwhale.utils.error import FormatError, ExistedError, NotFoundError


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

    def test_file_stat(self) -> None:
        file_path = Path("tmp/dir/file.txt")
        self.fs.create_file(file_path, contents="123456")
        stat = file_stat(file_path)
        assert stat.st_size == 6

    def test_cmp_file(self) -> None:
        base_file = Path("tmp/cmp/file1.txt")
        cmp_file = Path("tmp/cmp/file2.txt")
        ensure_dir("tmp/cmp")
        ensure_file(base_file, "123\n456\n")

        ensure_file(cmp_file, "456\n")
        diffs = cmp_file_content(base_file, cmp_file)
        assert len(diffs) == 1
        assert diffs == ["-123\n"]

        ensure_file(cmp_file, "1234\n456\n")
        diffs = cmp_file_content(base_file, cmp_file)
        assert len(diffs) == 2
        assert diffs == ["-123\n", "+1234\n"]

        ensure_file(cmp_file, "123\n456\n789\n")
        diffs = cmp_file_content(base_file, cmp_file)
        assert len(diffs) == 1
        assert diffs == ["+789\n"]

    def test_within_dir(self) -> None:
        cases = [
            ("/tmp/1", "/tmp/1/2", True),
            ("/tmp/1/2", "/tmp/1", False),
            ("/tmp/1", "/tmp/1/../../", False),
            (Path("/tmp/1"), "/tmp/1/2", True),
            ("/tmp/1/2", Path("/tmp/1"), False),
            (Path("/tmp/1"), Path("/tmp/1/2"), True),
            ("tmp/1", "tmp2/1/2", False),
            ("tmp/1", "tmp/1/2", True),
        ]

        for parent, child, expected in cases:
            assert expected == is_within_dir(parent, child)

    @patch("starwhale.utils.fs.tarfile.open")
    def test_extract_tar(self, m_open: MagicMock) -> None:
        root = Path("/home/starwhale")
        target_dir = root / "target"
        tar_path = root / "export.tar"

        with self.assertRaises(NotFoundError):
            extract_tar(tar_path, target_dir)

        ensure_dir(root)
        self.fs.create_file(str(tar_path), contents="")
        ensure_dir(target_dir)
        with self.assertRaises(ExistedError):
            extract_tar(tar_path, target_dir)

        valid_member = MagicMock()
        valid_member.configure_mock(name="in")

        invalid_member = MagicMock()
        invalid_member.configure_mock(name="../../out")

        m_open.return_value.__enter__.return_value.getmembers.return_value = [
            invalid_member,
            valid_member,
        ]
        with self.assertRaisesRegex(Exception, "Attempted path traversal in tar file"):
            extract_tar(tar_path, target_dir, force=True)

        m_open.reset_mock()
        m_open.return_value.__enter__.return_value.getmembers.return_value = [
            valid_member,
        ]
        extract_tar(tar_path, target_dir, force=True)
        assert m_open().__enter__().extractall.call_args[1] == {"path": str(target_dir)}
