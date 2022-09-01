import os
from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.api._impl.job import Parser
from starwhale.consts import DEFAULT_EVALUATION_PIPELINE, DEFAULT_EVALUATION_JOBS_FNAME
from starwhale.utils.fs import ensure_dir


class JobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()

    def test_generate_job_yaml(self):
        # create a temporary directory
        root = "home/starwhale/job"
        ensure_dir(root)
        _f = os.path.join(root, DEFAULT_EVALUATION_JOBS_FNAME)

        Parser.generate_job_yaml(DEFAULT_EVALUATION_PIPELINE, Path(root), Path(_f))

        """
        default:
        - concurrency: 1
          job_name: default
          needs: []
          resources:
          - cpu=1
          step_name: ppl
          task_num: 1
        - concurrency: 1
          job_name: default
          needs:
          - ppl
          resources:
          - cpu=1
          step_name: cmp
          task_num: 1
        """
        jobs = Parser.parse_job_from_yaml(_f)

        self.assertEqual("default" in jobs, True)
        self.assertEqual(len(jobs["default"]), 2)

    def test_scheduler(self):
        # TODO:
        pass
