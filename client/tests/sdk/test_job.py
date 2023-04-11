import sys
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

from starwhale.utils import load_yaml
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.error import NoSupportError
from starwhale.api._impl.job import (
    _jobs_global,
    pass_context,
    AFTER_LOAD_HOOKS,
    _validate_jobs_dag,
    generate_jobs_yaml,
    _do_resource_transform,
)
from starwhale.core.job.step import Step
from starwhale.core.job.task import TaskExecutor
from starwhale.core.job.context import Context
from starwhale.core.job.scheduler import Scheduler
from starwhale.api._impl.evaluation import (
    _registered_predict_func,
    _registered_evaluate_func,
)

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

    def test_validate_jobs_dag(self):
        with self.assertRaisesRegex(RuntimeError, "it will cause the graph to cycle"):
            _validate_jobs_dag(
                {
                    "default": [
                        Step(name="ppl", needs=[""]),
                        Step(name="cmp", needs=["cmp"]),
                    ]
                }
            )

        with self.assertRaisesRegex(
            RuntimeError, "Vertex 'not_found' does not belong to DAG"
        ):
            _validate_jobs_dag(
                {
                    "default": [
                        Step(name="ppl", needs=[""]),
                        Step(name="cmp", needs=["not_found"]),
                    ]
                }
            )

        _validate_jobs_dag(
            {
                "default": [
                    Step(name="ppl"),
                    Step(name="cmp", needs=["ppl"]),
                ]
            }
        )

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

        for resource, exception_str in exception_cases:
            with self.assertRaisesRegex(RuntimeError, exception_str):
                _do_resource_transform(resource)

        self.assertEqual(
            _do_resource_transform(
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
            _do_resource_transform(
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
        global _jobs_global
        if "default" in _jobs_global:
            _jobs_global.__delitem__("default")

    def tearDown(self) -> None:
        super().tearDown()

        if self.module_name in sys.modules:
            del sys.modules[self.module_name]

        path = str(self.workdir)
        if path in sys.path:
            sys.path.remove(path)

        _registered_predict_func.value = None
        _registered_evaluate_func.value = None

    def _ensure_py_script(self, content: str) -> None:
        ensure_dir(self.workdir)
        ensure_file(self.workdir / "__init__.py", "")
        ensure_file(self.workdir / f"{self.module_name}.py", content)

    @patch("starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_ppl")
    def test_predict_deco_on_function(self, mock_ppl: MagicMock) -> None:
        content = """
from starwhale import evaluation

@evaluation.predict
def predict_handler(*args, **kwargs): ...

@evaluation.evaluate
def evaluate_handler(*args, **kwargs): ...
        """
        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        assert self.module_name not in sys.modules
        generate_jobs_yaml(
            f"{self.module_name}:predict_handler", self.workdir, yaml_path
        )
        assert self.module_name in sys.modules
        assert len(AFTER_LOAD_HOOKS) == 1

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "default": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {
                        "dataset_uris": None,
                        "ignore_dataset_data": False,
                        "ignore_error": False,
                        "ppl_auto_log": True,
                        "ppl_batch_size": 1,
                    },
                    "func_name": "predict_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "predict",
                    "needs": [],
                    "resources": [],
                    "task_num": 2,
                },
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {"ppl_auto_log": True},
                    "func_name": "evaluate_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "evaluate",
                    "needs": ["predict"],
                    "resources": [],
                    "task_num": 1,
                },
            ]
        }

        steps = Step.get_steps_from_yaml("default", yaml_path)
        assert len(steps) == 2
        assert steps[0].name == "predict"
        assert steps[1].name == "evaluate"

        context = Context(
            workdir=self.workdir,
            project="test",
            version="123",
        )
        task = TaskExecutor(
            index=1, context=context, workdir=self.workdir, step=steps[0]
        )
        result = task.execute()
        assert result.status == "success"
        assert mock_ppl.call_count == 1

    @patch("starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_cmp")
    def test_pipeline_handler(self, mock_cmp: MagicMock) -> None:
        content = """
from starwhale import PipelineHandler

class MockHandler(PipelineHandler):
    def ppl(self, *args, **kwargs): ...
    def cmp(self, *args, **kwargs): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        assert self.module_name not in sys.modules
        generate_jobs_yaml(f"{self.module_name}:MockHandler", self.workdir, yaml_path)
        assert self.module_name in sys.modules
        assert len(AFTER_LOAD_HOOKS) == 1

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "default": [
                {
                    "cls_name": "MockHandler",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "ppl",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "ppl",
                    "needs": [],
                    "resources": [],
                    "task_num": 2,
                },
                {
                    "cls_name": "MockHandler",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "cmp",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "cmp",
                    "needs": ["ppl"],
                    "resources": [],
                    "task_num": 1,
                },
            ]
        }

        steps = Step.get_steps_from_yaml("default", yaml_path)
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

    @patch("starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_cmp")
    @patch("starwhale.api._impl.evaluation.PipelineHandler._starwhale_internal_run_ppl")
    def test_predict_deco_on_cls_method(
        self, mock_ppl: MagicMock, mock_cmp: MagicMock
    ) -> None:
        content = """
from starwhale import evaluation

class MockHandler:
    @evaluation.predict(task_num=4)
    def predict_handler(self, *args, **kwargs): ...

    @evaluation.evaluate(use_predict_auto_log=True)
    def evaluate_handler(self, *args, **kwargs): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        assert self.module_name not in sys.modules
        generate_jobs_yaml(f"{self.module_name}:MockHandler", self.workdir, yaml_path)
        assert self.module_name in sys.modules
        assert len(AFTER_LOAD_HOOKS) == 1

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "default": [
                {
                    "cls_name": "MockHandler",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {
                        "dataset_uris": None,
                        "ignore_dataset_data": False,
                        "ignore_error": False,
                        "ppl_auto_log": True,
                        "ppl_batch_size": 1,
                    },
                    "func_name": "predict_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "predict",
                    "needs": [],
                    "resources": [],
                    "task_num": 4,
                },
                {
                    "cls_name": "MockHandler",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {"ppl_auto_log": True},
                    "func_name": "evaluate_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "evaluate",
                    "needs": ["predict"],
                    "resources": [],
                    "task_num": 1,
                },
            ]
        }

        steps = Step.get_steps_from_yaml("default", yaml_path)
        results = Scheduler(
            project="test",
            version="test",
            workdir=self.workdir,
            dataset_uris=[],
            steps=steps,
        ).run()

        assert mock_ppl.call_count == 4
        assert mock_cmp.call_count == 1

        assert results[0].name == "predict"
        assert results[1].name == "evaluate"

        assert len(results[0].task_results) == 4
        assert len(results[1].task_results) == 1

    def test_step_deco(self) -> None:
        content = """
from starwhale import step

@step(name="prepare", task_num=1)
def prepare_handler(): ...

@step(name="evaluate", task_num=1, needs=["predict"])
def evaluate_handler(): ...

@step(name="predict", task_num=10, needs=["prepare"])
def predict_handler(): ...

class MockReport:
    @step(name="report", task_num=1, needs=["evaluate"])
    def report_handler(self): ...
        """

        self._ensure_py_script(content)
        yaml_path = self.workdir / "job.yaml"
        generate_jobs_yaml(self.module_name, self.workdir, yaml_path)
        assert len(AFTER_LOAD_HOOKS) == 1

        assert yaml_path.exists()
        jobs_info = load_yaml(yaml_path)
        assert jobs_info == {
            "default": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "prepare_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "prepare",
                    "needs": [],
                    "resources": [],
                    "task_num": 1,
                },
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "evaluate_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "evaluate",
                    "needs": ["predict"],
                    "resources": [],
                    "task_num": 1,
                },
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "predict_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "predict",
                    "needs": ["prepare"],
                    "resources": [],
                    "task_num": 10,
                },
                {
                    "cls_name": "MockReport",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "report_handler",
                    "job_name": "default",
                    "module_name": self.module_name,
                    "name": "report",
                    "needs": ["evaluate"],
                    "resources": [],
                    "task_num": 1,
                },
            ]
        }

        steps = Step.get_steps_from_yaml("default", yaml_path)
        results = Scheduler(
            project="test",
            version="test",
            workdir=self.workdir,
            dataset_uris=[],
            steps=steps,
        ).run()

        assert ["prepare", "predict", "evaluate", "report"] == [r.name for r in results]
        assert all([r.status == "success" for r in results])
        assert [1, 10, 1, 1] == [len(r.task_results) for r in results]

    def test_no_jobs(self) -> None:
        content = """
mock_var = 1
def mock_func(): ...

class MockCls: ...
        """
        self._ensure_py_script(content)

        assert len(_jobs_global) == 0

        handlers = (
            f"{self.module_name}:MockCls",
            self.module_name,
        )

        for handler in handlers:
            with self.assertRaisesRegex(RuntimeError, "not found any jobs"):
                generate_jobs_yaml(handler, self.workdir, "not_found.yaml")

        with self.assertRaisesRegex(
            RuntimeError,
            "preload function-mock_func does not use step or predict decorator",
        ):
            generate_jobs_yaml(
                f"{self.module_name}:mock_func", self.workdir, "not_found.yaml"
            )

        with self.assertRaisesRegex(
            NoSupportError, f"failed to preload for {self.module_name}:mock_var"
        ):
            generate_jobs_yaml(
                f"{self.module_name}:mock_var", self.workdir, "not_found.yaml"
            )

    def test_step_no_support_on_cls(self) -> None:
        content = """
from starwhale import step

@step()
class MockHandler:
    def __call__(self, *args, **kwargs): ...
        """
        self._ensure_py_script(content)

        handlers = [self.module_name, f"{self.module_name}:MockHandler"]

        for hdl in handlers:
            with self.assertRaisesRegex(
                NoSupportError, "step decorator no support class"
            ):
                generate_jobs_yaml(hdl, self.workdir, "not_found.yaml")

    def test_step_no_support_on_cls_inner_func(self) -> None:
        content = """
from starwhale import step

class MockCls:
    class _InnerCls:
        @step()
        def handler(self, *args, **kwargs): ...
        """
        self._ensure_py_script(content)

        with self.assertRaisesRegex(
            NoSupportError, "step decorator no supports inner class method"
        ):
            generate_jobs_yaml(self.module_name, self.workdir, "not_found.yaml")
