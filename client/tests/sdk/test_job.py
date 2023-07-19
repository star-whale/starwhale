import sys
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

from starwhale.utils import load_yaml
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.error import NoSupportError
from starwhale.base.context import Context, pass_context
from starwhale.api._impl.job import Handler, generate_jobs_yaml
from starwhale.base.scheduler import Step, Scheduler, TaskExecutor

from .. import BaseTestCase


class JobTestCase(unittest.TestCase):
    def test_pass_context(self):
        config_context = Context(
            workdir=Path(), step="self_test", version="qwertyui", project="self"
        )
        Context.set_runtime_context(config_context)

        @pass_context
        def my_step(context: Context):
            self.assertEqual(context, config_context)

        my_step()

    def test_resource_transform(self):
        exception_cases = [
            ({"ppu": 1}, "resources name is illegal"),
            (
                {"cpu": {"res": 1, "limit": 2}},
                "resources value is illegal, attribute's name must in",
            ),
            (
                {"cpu": {"request": "u", "limit": 2}},
                "resource:cpu only support type",
            ),
            (
                {
                    "cpu": 0.1,
                    "memory": 2,
                    "nvidia.com/gpu": 2.1,
                },
                "resource:nvidia.com/gpu only support type",
            ),
            (
                {
                    "cpu": {
                        "request": 0.1,
                        "limit": 0.2,
                    },
                    "memory": 2,
                    "nvidia.com/gpu": {  # gpu don't support float
                        "request": 0.1,
                        "limit": 0.2,
                    },
                },
                "resource:nvidia.com/gpu only support type",
            ),
            (
                {
                    "cpu": "0.1",
                    "memory": "100",
                    "nvidia.com/gpu": "1",
                },
                "resources value is illegal, attribute's type must be number or dict",
            ),
            (
                {
                    "cpu": 0.1,
                    "memory": -100,
                    "nvidia.com/gpu": 1,
                },
                "request only supports non-negative number, but now is -100",
            ),
        ]

        mock_handler = Handler("", "", "", "")
        for resource, exception_str in exception_cases:
            with self.assertRaisesRegex(RuntimeError, exception_str):
                mock_handler._transform_resource(resource)

        self.assertEqual(
            mock_handler._transform_resource(
                {
                    "cpu": 0.1,
                    "memory": 100,
                    "nvidia.com/gpu": 1,
                }
            ),
            [
                {
                    "type": "cpu",
                    "request": 0.1,
                    "limit": 0.1,
                },
                {
                    "type": "memory",
                    "request": 100,
                    "limit": 100,
                },
                {
                    "type": "nvidia.com/gpu",
                    "request": 1,
                    "limit": 1,
                },
            ],
        )
        self.assertEqual(
            mock_handler._transform_resource(
                {
                    "cpu": {
                        "request": 0.1,
                        "limit": 0.2,
                    },
                    "memory": {
                        "request": 100.1,
                        "limit": 100.2,
                    },
                    "nvidia.com/gpu": {
                        "request": 1,
                        "limit": 2,
                    },
                }
            ),
            [
                {
                    "type": "cpu",
                    "request": 0.1,
                    "limit": 0.2,
                },
                {
                    "type": "memory",
                    "request": 100.1,
                    "limit": 100.2,
                },
                {
                    "type": "nvidia.com/gpu",
                    "request": 1,
                    "limit": 2,
                },
            ],
        )


class GenerateJobsTestCase(BaseTestCase):
    def setUp(self):
        super().setUp()
        self.workdir = Path(self.local_storage) / "scripts"
        self.module_name = "mock_user_module"

        keys = list(Handler._registered_handlers.keys())
        for k in keys:
            Handler._registered_handlers.__delitem__(k)

    def tearDown(self) -> None:
        super().tearDown()

        if self.module_name in sys.modules:
            del sys.modules[self.module_name]

        path = str(self.workdir)
        if path in sys.path:
            sys.path.remove(path)

    def _ensure_py_script(self, content: str) -> None:
        ensure_dir(self.workdir)
        ensure_file(self.workdir / "__init__.py", "")
        ensure_file(self.workdir / f"{self.module_name}.py", content)

    def test_multi_predict_decorators(self) -> None:
        content = """
from starwhale import evaluation

@evaluation.predict
def img_predict_handler(*args, **kwargs): ...

@evaluation.evaluate(needs=[img_predict_handler])
def img_evaluate_handler(*args, **kwargs): ...

@evaluation.predict
def video_predict_handler(*args, **kwargs): ...

@evaluation.evaluate(needs=[video_predict_handler])
def video_evaluate_handler(*args, **kwargs): ...
        """
        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml([f"{self.module_name}"], self.workdir, yaml_path)

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)

        assert {
            "mock_user_module:img_evaluate_handler",
            "mock_user_module:img_predict_handler",
            "mock_user_module:video_evaluate_handler",
            "mock_user_module:video_predict_handler",
        } == set(jobs_info.keys())
        assert jobs_info["mock_user_module:img_evaluate_handler"] == [
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {
                    "dataset_uris": None,
                    "ignore_error": False,
                    "predict_auto_log": True,
                    "predict_batch_size": 1,
                    "predict_log_mode": "pickle",
                    "predict_log_dataset_features": None,
                },
                "func_name": "img_predict_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:img_predict_handler",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": True,
            },
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {"predict_auto_log": True},
                "func_name": "img_evaluate_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:img_evaluate_handler",
                "needs": ["mock_user_module:img_predict_handler"],
                "replicas": 1,
                "resources": [],
                "show_name": "evaluate",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
        ]

        assert jobs_info["mock_user_module:video_evaluate_handler"] == [
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {
                    "dataset_uris": None,
                    "ignore_error": False,
                    "predict_auto_log": True,
                    "predict_batch_size": 1,
                    "predict_log_mode": "pickle",
                    "predict_log_dataset_features": None,
                },
                "func_name": "video_predict_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:video_predict_handler",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": True,
            },
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {"predict_auto_log": True},
                "func_name": "video_evaluate_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:video_evaluate_handler",
                "needs": ["mock_user_module:video_predict_handler"],
                "replicas": 1,
                "resources": [],
                "show_name": "evaluate",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
        ]

    @patch(
        "starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_predict"
    )
    def test_predict_deco_on_function(self, mock_ppl: MagicMock) -> None:
        content = """
from starwhale import evaluation

@evaluation.predict(log_mode="pickle", replicas=2)
def predict_handler(data): ...

@evaluation.evaluate(needs=[predict_handler])
def evaluate_handler(*args, **kwargs): ...
        """
        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        assert self.module_name not in sys.modules
        generate_jobs_yaml(
            [f"{self.module_name}:predict_handler"], self.workdir, yaml_path
        )
        assert self.module_name in sys.modules

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "mock_user_module:evaluate_handler": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {
                        "dataset_uris": None,
                        "ignore_error": False,
                        "predict_auto_log": True,
                        "predict_batch_size": 1,
                        "predict_log_mode": "pickle",
                        "predict_log_dataset_features": None,
                    },
                    "func_name": "predict_handler",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:predict_handler",
                    "needs": [],
                    "replicas": 2,
                    "resources": [],
                    "show_name": "predict",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": True,
                },
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {"predict_auto_log": True},
                    "func_name": "evaluate_handler",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:evaluate_handler",
                    "needs": ["mock_user_module:predict_handler"],
                    "replicas": 1,
                    "resources": [],
                    "show_name": "evaluate",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": False,
                },
            ],
            "mock_user_module:predict_handler": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {
                        "dataset_uris": None,
                        "ignore_error": False,
                        "predict_auto_log": True,
                        "predict_batch_size": 1,
                        "predict_log_mode": "pickle",
                        "predict_log_dataset_features": None,
                    },
                    "func_name": "predict_handler",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:predict_handler",
                    "needs": [],
                    "replicas": 2,
                    "resources": [],
                    "show_name": "predict",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": True,
                }
            ],
        }
        steps = Step.get_steps_from_yaml("mock_user_module:evaluate_handler", yaml_path)
        assert len(steps) == 2
        assert steps[0].name == "mock_user_module:predict_handler"
        assert steps[0].show_name == "predict"
        assert steps[1].name == "mock_user_module:evaluate_handler"
        assert steps[1].show_name == "evaluate"

        steps = Step.get_steps_from_yaml("mock_user_module:predict_handler", yaml_path)
        assert len(steps) == 1
        assert steps[0].name == "mock_user_module:predict_handler"
        assert steps[0].show_name == "predict"
        assert steps[0].require_dataset is True

        steps = Step.get_steps_from_yaml("", yaml_path)
        assert len(steps) == 2

        steps = Step.get_steps_from_yaml("0", yaml_path)
        assert len(steps) == 2

        steps = Step.get_steps_from_yaml(1, yaml_path)
        assert len(steps) == 1
        assert steps[0].name == "mock_user_module:predict_handler"
        assert steps[0].show_name == "predict"
        assert steps[0].require_dataset is True

        with self.assertRaises(RuntimeError):
            # without dataset_uri
            context = Context(
                workdir=self.workdir,
                project="test",
                version="123",
            )
            TaskExecutor(index=1, context=context, workdir=self.workdir, step=steps[0])

        context = Context(
            workdir=self.workdir,
            project="test",
            version="123",
            dataset_uris=["ds/version/v0"],
        )
        task = TaskExecutor(
            index=1, context=context, workdir=self.workdir, step=steps[0]
        )
        result = task.execute()
        assert result.status == "success"
        assert mock_ppl.call_count == 1

        with self.assertRaises(IndexError):
            Step.get_steps_from_yaml(3, yaml_path)

    @patch(
        "starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_evaluate"
    )
    def test_pipeline_handler_with_ppl_cmp(self, mock_cmp: MagicMock) -> None:
        content = """
from starwhale import PipelineHandler

class MockPPLHandler(PipelineHandler):
    def ppl(self, data, **kw): ...
    def cmp(self, *args, **kwargs): ...
        """
        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml(
            [f"{self.module_name}:MockPPLHandler"], self.workdir, yaml_path
        )
        jobs_info = load_yaml(yaml_path)
        assert "mock_user_module:MockPPLHandler.cmp" in jobs_info
        assert "mock_user_module:MockPPLHandler.ppl" in jobs_info
        assert jobs_info["mock_user_module:MockPPLHandler.cmp"][1]["needs"] == [
            "mock_user_module:MockPPLHandler.ppl"
        ]

    @patch(
        "starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_evaluate"
    )
    def test_pipeline_handler(self, mock_cmp: MagicMock) -> None:
        content = """
from starwhale import PipelineHandler

class MockHandler(PipelineHandler):
    def predict(self, *args): ...
    def evaluate(self, *args, **kwargs): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        assert self.module_name not in sys.modules
        generate_jobs_yaml([f"{self.module_name}:MockHandler"], self.workdir, yaml_path)
        assert self.module_name in sys.modules

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info["mock_user_module:MockHandler.evaluate"] == [
            {
                "cls_name": "MockHandler",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "predict",
                "module_name": "mock_user_module",
                "name": "mock_user_module:MockHandler.predict",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": True,
            },
            {
                "cls_name": "MockHandler",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "evaluate",
                "module_name": "mock_user_module",
                "name": "mock_user_module:MockHandler.evaluate",
                "needs": ["mock_user_module:MockHandler.predict"],
                "replicas": 1,
                "resources": [],
                "show_name": "evaluate",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
        ]
        assert jobs_info["mock_user_module:MockHandler.predict"] == [
            {
                "cls_name": "MockHandler",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "predict",
                "module_name": "mock_user_module",
                "name": "mock_user_module:MockHandler.predict",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            }
        ]
        steps = Step.get_steps_from_yaml(
            "mock_user_module:MockHandler.evaluate", yaml_path
        )
        context = Context(
            workdir=self.workdir,
            project="test",
            version="123",
        )
        task = TaskExecutor(
            index=1, context=context, workdir=self.workdir, step=steps[1]
        )
        result = task.execute()
        assert result.status == "success"
        assert mock_cmp.call_count == 1

    @patch(
        "starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_evaluate"
    )
    @patch(
        "starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_predict"
    )
    def test_predict_deco_on_cls_method(
        self, mock_ppl: MagicMock, mock_cmp: MagicMock
    ) -> None:
        content = """
from starwhale import evaluation

class MockHandler:
    @evaluation.predict(replicas=4, log_mode="plain")
    def predict_handler(self, **kwargs): ...

    @evaluation.evaluate(use_predict_auto_log=True, needs=[predict_handler])
    def evaluate_handler(self, *args, **kwargs): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        assert self.module_name not in sys.modules
        generate_jobs_yaml([f"{self.module_name}:MockHandler"], self.workdir, yaml_path)
        assert self.module_name in sys.modules

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert len(jobs_info) == 2
        assert jobs_info["mock_user_module:MockHandler.predict_handler"] == [
            {
                "cls_name": "MockHandler",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {
                    "dataset_uris": None,
                    "ignore_error": False,
                    "predict_auto_log": True,
                    "predict_batch_size": 1,
                    "predict_log_mode": "plain",
                    "predict_log_dataset_features": None,
                },
                "func_name": "predict_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:MockHandler.predict_handler",
                "needs": [],
                "replicas": 4,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": True,
            }
        ]

        assert jobs_info["mock_user_module:MockHandler.evaluate_handler"] == [
            {
                "cls_name": "MockHandler",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {
                    "dataset_uris": None,
                    "ignore_error": False,
                    "predict_auto_log": True,
                    "predict_batch_size": 1,
                    "predict_log_mode": "plain",
                    "predict_log_dataset_features": None,
                },
                "func_name": "predict_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:MockHandler.predict_handler",
                "needs": [],
                "replicas": 4,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": True,
            },
            {
                "cls_name": "MockHandler",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {"predict_auto_log": True},
                "func_name": "evaluate_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:MockHandler.evaluate_handler",
                "needs": ["mock_user_module:MockHandler.predict_handler"],
                "replicas": 1,
                "resources": [],
                "show_name": "evaluate",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
        ]

        steps = Step.get_steps_from_yaml(
            "mock_user_module:MockHandler.evaluate_handler", yaml_path
        )
        assert len(steps) == 2
        results = Scheduler(
            project="test",
            version="test",
            workdir=self.workdir,
            dataset_uris=["ds/version/v0"],
            steps=steps,
        ).run()

        assert mock_ppl.call_count == 4
        assert mock_cmp.call_count == 1

        assert results[0].name == "mock_user_module:MockHandler.predict_handler"
        assert results[1].name == "mock_user_module:MockHandler.evaluate_handler"

        assert len(results[0].task_results) == 4
        assert len(results[1].task_results) == 1

    def test_no_needs(self) -> None:
        content = """
from starwhale import handler

@handler(name="run", replicas=10)
def run(): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml([self.module_name], self.workdir, yaml_path)
        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "mock_user_module:run": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "run",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:run",
                    "needs": [],
                    "replicas": 10,
                    "resources": [],
                    "show_name": "run",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": False,
                }
            ]
        }

    def test_handler_with_other_decorator(self) -> None:
        content = """
from starwhale import handler, pass_context

@handler(replicas=2)
@pass_context
def handle(context): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml([self.module_name], self.workdir, yaml_path)

        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "mock_user_module:handle": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "handle",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:handle",
                    "needs": [],
                    "replicas": 2,
                    "resources": [],
                    "show_name": "handle",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": False,
                }
            ]
        }

    def test_fine_tune_deco(self) -> None:
        content = """
from starwhale import fine_tune

@fine_tune
def ft1(): ...

@fine_tune(needs=[ft1], resources={'nvidia.com/gpu': 1})
def ft2(): ...
"""
        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml([self.module_name], self.workdir, yaml_path)

        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "mock_user_module:ft1": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "ft1",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:ft1",
                    "needs": [],
                    "replicas": 1,
                    "resources": [],
                    "show_name": "fine_tune",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": True,
                }
            ],
            "mock_user_module:ft2": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "ft2",
                    "module_name": "mock_user_module",
                    "name": "mock_user_module:ft2",
                    "needs": [],
                    "replicas": 1,
                    "resources": [{"limit": 1, "request": 1, "type": "nvidia.com/gpu"}],
                    "show_name": "fine_tune",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": True,
                }
            ],
        }

    def test_handler_deco(self) -> None:
        content = """
from starwhale import handler

@handler(name="prepare", replicas=1)
def prepare_handler(): ...

@handler(name="evaluate", replicas=1, needs=[prepare_handler])
def evaluate_handler(): ...

@handler(name="predict", replicas=10, needs=[prepare_handler])
def predict_handler(): ...

class MockReport:
    @handler(name="report", replicas=1, needs=[evaluate_handler, predict_handler])
    def report_handler(self): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml([self.module_name], self.workdir, yaml_path)

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)

        report_handler = jobs_info["mock_user_module:MockReport.report_handler"]
        assert len(report_handler) == 4
        assert {
            "cls_name": "",
            "concurrency": 1,
            "extra_args": [],
            "extra_kwargs": {},
            "func_name": "prepare_handler",
            "module_name": "mock_user_module",
            "name": "mock_user_module:prepare_handler",
            "needs": [],
            "replicas": 1,
            "resources": [],
            "show_name": "prepare",
            "expose": 0,
            "virtual": False,
            "require_dataset": False,
        } in report_handler

        assert {
            "cls_name": "",
            "concurrency": 1,
            "extra_args": [],
            "extra_kwargs": {},
            "func_name": "evaluate_handler",
            "module_name": "mock_user_module",
            "name": "mock_user_module:evaluate_handler",
            "needs": ["mock_user_module:prepare_handler"],
            "replicas": 1,
            "resources": [],
            "show_name": "evaluate",
            "expose": 0,
            "virtual": False,
            "require_dataset": False,
        } in report_handler

        assert {
            "cls_name": "MockReport",
            "concurrency": 1,
            "extra_args": [],
            "extra_kwargs": {},
            "func_name": "report_handler",
            "module_name": "mock_user_module",
            "name": "mock_user_module:MockReport.report_handler",
            "needs": [
                "mock_user_module:evaluate_handler",
                "mock_user_module:predict_handler",
            ],
            "replicas": 1,
            "resources": [],
            "show_name": "report",
            "expose": 0,
            "virtual": False,
            "require_dataset": False,
        } in report_handler

        assert {
            "cls_name": "",
            "concurrency": 1,
            "extra_args": [],
            "extra_kwargs": {},
            "func_name": "predict_handler",
            "module_name": "mock_user_module",
            "name": "mock_user_module:predict_handler",
            "needs": ["mock_user_module:prepare_handler"],
            "replicas": 10,
            "resources": [],
            "show_name": "predict",
            "expose": 0,
            "virtual": False,
            "require_dataset": False,
        } in report_handler

        assert jobs_info["mock_user_module:evaluate_handler"] == [
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "prepare_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:prepare_handler",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "prepare",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "evaluate_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:evaluate_handler",
                "needs": ["mock_user_module:prepare_handler"],
                "replicas": 1,
                "resources": [],
                "show_name": "evaluate",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
        ]
        assert jobs_info["mock_user_module:predict_handler"] == [
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "prepare_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:prepare_handler",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "prepare",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "predict_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:predict_handler",
                "needs": ["mock_user_module:prepare_handler"],
                "replicas": 10,
                "resources": [],
                "show_name": "predict",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            },
        ]
        assert jobs_info["mock_user_module:prepare_handler"] == [
            {
                "cls_name": "",
                "concurrency": 1,
                "extra_args": [],
                "extra_kwargs": {},
                "func_name": "prepare_handler",
                "module_name": "mock_user_module",
                "name": "mock_user_module:prepare_handler",
                "needs": [],
                "replicas": 1,
                "resources": [],
                "show_name": "prepare",
                "expose": 0,
                "virtual": False,
                "require_dataset": False,
            }
        ]

        steps = Step.get_steps_from_yaml(
            "mock_user_module:MockReport.report_handler", yaml_path
        )
        scheduler = Scheduler(
            project="test",
            version="test",
            workdir=self.workdir,
            dataset_uris=[],
            steps=steps,
        )
        results = scheduler.run()
        assert {
            "mock_user_module:prepare_handler",
            "mock_user_module:evaluate_handler",
            "mock_user_module:predict_handler",
            "mock_user_module:MockReport.report_handler",
        } == {r.name for r in results}
        assert all([r.status == "success" for r in results])
        assert {1, 1, 10, 1} == {len(r.task_results) for r in results}
        for r in results:
            if r.name == "mock_user_module:predict_handler":
                assert {i for i in range(10)} == {t.id for t in r.task_results}

        results = scheduler.run(
            step_name="mock_user_module:predict_handler",
            task_index=3,
            task_num=12,
        )
        assert {1} == {len(r.task_results) for r in results}
        assert {"mock_user_module:predict_handler"} == {r.name for r in results}
        assert len(results) == 1
        assert len(results[0].task_results) == 1
        assert results[0].task_results[0].id == 3

        results = scheduler.run(
            step_name="mock_user_module:predict_handler",
        )
        assert {10} == {len(r.task_results) for r in results}
        assert {"mock_user_module:predict_handler"} == {r.name for r in results}
        assert len(results) == 1
        assert len(results[0].task_results) == 10
        assert {i for i in range(10)} == {t.id for t in results[0].task_results}

        results = scheduler.run(
            step_name="mock_user_module:predict_handler",
            task_num=3,
        )
        assert {3} == {len(r.task_results) for r in results}
        assert {"mock_user_module:predict_handler"} == {r.name for r in results}
        assert len(results) == 1
        assert len(results[0].task_results) == 3
        assert {i for i in range(3)} == {t.id for t in results[0].task_results}

        results = scheduler.run(
            step_name="mock_user_module:predict_handler",
            task_index=5,
        )
        assert {1} == {len(r.task_results) for r in results}
        assert {"mock_user_module:predict_handler"} == {r.name for r in results}
        assert len(results) == 1
        assert len(results[0].task_results) == 1
        assert results[0].task_results[0].id == 5

        with self.assertRaisesRegex(RuntimeError, "out of bounds"):
            scheduler.run(
                step_name="mock_user_module:predict_handler",
                task_index=10,
                task_num=1,
            )

    def test_evaluator_needs(self) -> None:
        content = """
from starwhale import evaluation

@evaluation.evaluate()
def evaluate_handler(): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        with self.assertRaisesRegex(
            ValueError, "needs is required for evaluate function"
        ):
            generate_jobs_yaml([f"{self.module_name}"], self.workdir, yaml_path)

    def test_no_jobs(self) -> None:
        content = """
mock_var = 1
def mock_func(): ...

class MockCls: ...
        """
        self._ensure_py_script(content)

        assert len(Handler._registered_handlers) == 0

        handlers = (
            f"{self.module_name}:MockCls",
            self.module_name,
        )

        for handler in handlers:
            with self.assertRaisesRegex(RuntimeError, "not found any handlers"):
                generate_jobs_yaml([handler], self.workdir, "not_found.yaml")

        with self.assertRaisesRegex(RuntimeError, "not found any handlers"):
            generate_jobs_yaml(
                [f"{self.module_name}:mock_func"], self.workdir, "not_found.yaml"
            )

    def test_step_no_support_on_cls(self) -> None:
        content = """
from starwhale import handler

@handler()
class MockHandler:
    def __call__(self, *args, **kwargs): ...
        """
        self._ensure_py_script(content)

        handlers = [self.module_name, f"{self.module_name}:MockHandler"]

        for hdl in handlers:
            with self.assertRaisesRegex(
                NoSupportError, "handler decorator only supports on function"
            ):
                generate_jobs_yaml([hdl], self.workdir, "not_found.yaml")

    def test_step_no_support_on_cls_inner_func(self) -> None:
        content = """
from starwhale import handler

class MockCls:
    class _InnerCls:
        @handler()
        def handler(self, *args, **kwargs): ...
        """
        self._ensure_py_script(content)

        with self.assertRaisesRegex(
            NoSupportError, "handler decorator no supports inner class method"
        ):
            generate_jobs_yaml([self.module_name], self.workdir, "not_found.yaml")

    def test_needs_on_normal_function(self) -> None:
        content = """
from starwhale import handler

def normal(): ...

@handler(needs=[normal])
def predict_handler(): ...
"""
        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        with self.assertRaisesRegex(RuntimeError, "dependency not found"):
            generate_jobs_yaml([self.module_name], self.workdir, yaml_path)
