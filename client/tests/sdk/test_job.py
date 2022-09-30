from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale import Context, pass_context
from starwhale.consts import thread_local


class JobTestCase(TestCase):
    def setUp(self) -> None:
        thread_local.context = Context(
            workdir=Path(), step="self_test", version="qwertyui", project="self"
        )

    def test_scheduler(self):
        @pass_context
        def my_step(context: Context):
            assert context
            self.assertEqual(context.step, "self_test")

        my_step()
