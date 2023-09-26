import queue
import struct
import typing as t
from pathlib import Path
from binascii import crc32

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.base.artifact import RotatedBinWriter


class TestRotatedBinWriter(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_bin_format(self) -> None:
        workdir = Path("/home/test")
        content = b"123456"
        alignment_size = 64

        with RotatedBinWriter(workdir, alignment_bytes_size=alignment_size) as w:
            w.write(content)

        bin_path = list(workdir.iterdir())[0]
        bin_content = bin_path.read_bytes()
        assert len(bin_content) == alignment_size

        groups = struct.unpack(">IIQIIII", bin_content[0:32])
        assert len(groups) == 7
        assert struct.pack(">I", groups[0]) == b"SWDS"  # header magic
        assert struct.pack(">I", groups[-1]) == b"SDWS"  # data magic
        assert groups[1] == crc32(content)  # data crc32
        assert groups[2] == 0  # reserved
        assert groups[3] == len(content)  # size
        assert groups[4] == alignment_size - len(content) - 32  # padding size
        assert groups[5] == 0  # header version

        c_start, c_end = 32, 32 + len(content)
        assert bin_content[c_start:c_end] == content
        assert bin_content[c_end:] == b"\0" * groups[4]

    def test_write_one_bin(self) -> None:
        workdir = Path("/home/test")
        content = b"abcdef"

        assert not workdir.exists()

        rbw = RotatedBinWriter(workdir, alignment_bytes_size=1, volume_bytes_size=100)
        assert not rbw._current_writer.closed
        bin_path, bin_section = rbw.write(content)
        assert rbw._wrote_size == bin_section.size
        assert rbw.working_path == bin_path
        rbw.close()

        assert bin_section.offset == 0
        assert bin_section.size == len(content) + RotatedBinWriter._header_size
        assert bin_section.raw_data_offset == RotatedBinWriter._header_size
        assert bin_section.raw_data_size == len(content)

        assert rbw.working_path != bin_path
        assert rbw._wrote_size == 0

        assert workdir.exists()
        assert bin_path.exists()
        assert rbw.rotated_paths == [bin_path] == list(workdir.iterdir())
        assert bin_path.parent == workdir
        assert rbw._current_writer.closed

    def test_write_multi_bins(self) -> None:
        workdir = Path("/home/test")
        rbw = RotatedBinWriter(workdir, alignment_bytes_size=1, volume_bytes_size=1)
        cnt = 10
        for _ in range(0, cnt):
            rbw.write(b"\0")
        rbw.close()
        assert len(rbw.rotated_paths) == cnt
        assert set(rbw.rotated_paths) == set(workdir.iterdir())
        assert rbw.working_path not in rbw.rotated_paths

    def test_notify(self) -> None:
        notify_queue = queue.Queue()

        cnt = 10
        assert notify_queue.qsize() == 0
        with RotatedBinWriter(
            Path("/home/test"),
            alignment_bytes_size=1,
            volume_bytes_size=1,
            rotated_bin_notify_queue=notify_queue,
        ) as w:
            for i in range(0, cnt):
                w.write(b"\0")
                assert notify_queue.qsize() == i + 1

        queue_paths = [notify_queue.get() for _ in range(0, cnt)]
        assert queue_paths == w.rotated_paths

    def test_close(self) -> None:
        rbw = RotatedBinWriter(Path("/home/test"))
        rbw.close()

        rbw = RotatedBinWriter(Path("/home/test"))
        rbw.write(b"123")
        rbw.close()

        assert rbw._current_writer.closed
        with self.assertRaisesRegex(ValueError, "I/O operation on closed file"):
            rbw.close()

    def test_alignment(self) -> None:
        class _M(t.NamedTuple):
            content_size: int
            alignment_size: int
            expected_bin_size: int

        cases = [
            _M(0, 1, 32),
            _M(0, 31, 62),
            _M(0, 64, 64),
            _M(1, 1, 33),
            _M(3, 1, 35),
            _M(32, 1, 64),
            _M(32, 63, 126),
            _M(32, 64, 64),
            _M(32, 32, 64),
            _M(32, 65, 65),
            _M(16, 16, 48),
            _M(16, 4096, 4096),
        ]

        for index, meta in enumerate(cases):
            workdir = Path(f"/home/test/{index}")
            with RotatedBinWriter(
                workdir, alignment_bytes_size=meta.alignment_size
            ) as w:
                w.write(b"\0" * meta.content_size)

            self.assertEqual(
                list(workdir.iterdir())[0].stat().st_size,
                meta.expected_bin_size,
                msg=f"content:{meta.content_size}, alignment:{meta.alignment_size}, expected:{meta.expected_bin_size}",
            )

        with self.assertRaisesRegex(
            ValueError, "alignment_bytes_size must be greater than zero"
        ):
            RotatedBinWriter(Path("."), alignment_bytes_size=0)
