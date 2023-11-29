import os
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

from requests_mock import Mocker

from tests import BaseTestCase
from starwhale.utils import load_yaml
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.load import import_object
from starwhale.api._impl.job import Handler
from starwhale.api._impl.model import build, _called_build_functions
from starwhale.base.models.base import obj_to_model
from starwhale.base.models.model import JobHandlers, StepSpecClient


class ModelBuildTestCase(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        self.old_cwd = os.getcwd()

    def tearDown(self) -> None:
        os.chdir(self.old_cwd)

    def test_build_exceptions(self) -> None:
        with self.assertRaisesRegex(
            ValueError, "is not in the subpath of|does not start with"
        ):
            build(
                [ModelBuildTestCase],
                workdir="not_found",
            )

        with self.assertRaisesRegex(TypeError, "is a built-in class"):
            build([list])

        with self.assertRaisesRegex(RuntimeError, "no modules to search"):
            Handler.clear_registered_handlers()
            build()

    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    @patch("starwhale.core.model.view.ModelTermView")
    def test_build_with_cwd(
        self, m_model_view: MagicMock, m_check_python: MagicMock
    ) -> None:
        m_check_python.return_value = [True, None, None]
        workdir = Path(self.local_storage) / "user" / "workdir"
        ensure_dir(workdir)
        ensure_file(workdir / "cwd_evaluator.py", "class Handler:...")

        mock_handler = import_object(
            workdir=workdir, handler_path="cwd_evaluator:Handler", py_env="venv"
        )
        os.chdir(workdir)
        build([mock_handler])

        model_config = m_model_view.build.call_args[1]["model_config"]
        assert model_config.name == "workdir"
        assert model_config.run.modules == ["cwd_evaluator"]
        assert (
            m_model_view.build.call_args[1]["workdir"]
        ).resolve().absolute() == workdir.resolve().absolute()

    @Mocker()
    @patch("os.unlink", MagicMock())
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    @patch("starwhale.core.model.view.ModelTermView")
    def test_build_with_copy(
        self,
        rm: Mocker,
        m_model_view: MagicMock,
        m_check_python: MagicMock,
        m_conf: MagicMock,
    ) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "foo": {
                    "uri": "http://localhost:8080",
                    "current_project": "test",
                    "sw_token": "token",
                },
                "local": {"uri": "local", "current_project": "self"},
            },
            "storage": {"root": "/root"},
        }
        rm.get(
            "http://localhost:8080/api/v1/project/starwhale", json={"data": {"id": 1}}
        )

        m_check_python.return_value = [True, None, None]
        workdir = Path(self.local_storage) / "copy" / "workdir"
        ensure_dir(workdir / "models")
        model_fpath = workdir / "models" / "mnist_cnn.pt"
        ensure_file(model_fpath, "")
        ensure_file(workdir / "__init__.py", "")
        evaluator_fpath = workdir / "evaluator_copy.py"
        ensure_file(evaluator_fpath, "class Handler:...")

        mock_handler = import_object(
            workdir=workdir, handler_path="evaluator_copy:Handler", py_env="venv"
        )

        with patch.dict(
            os.environ, {"SW_INSTANCE_URI": "cloud://server", "SW_PROJECT": "starwhale"}
        ):
            build(name="test", workdir=workdir, modules=[mock_handler])
            assert (
                m_model_view.copy.call_args[1]["src_uri"]
                == "local/project/self/model/test/version/latest"
            )
            assert (
                m_model_view.copy.call_args[1]["dest_uri"]
                == "cloud://server/project/starwhale"
            )

        build(
            modules=[mock_handler],
            name="test",
            workdir=workdir,
            project_uri="local/project/self",
            remote_project_uri="http://localhost:8080/project/starwhale",
        )
        assert (
            m_model_view.copy.call_args[1]["src_uri"]
            == "local/project/self/model/test/version/latest"
        )
        assert (
            m_model_view.copy.call_args[1]["dest_uri"]
            == "http://localhost:8080/project/1"
        )

    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    def test_built_in_cycle_call(self, m_check_python: MagicMock) -> None:
        m_check_python.return_value = [True, None, None]
        workdir = Path(self.local_storage) / "user" / "workdir"
        ensure_file(workdir / "__init__.py", "", parents=True)
        content = """

from pathlib import Path
from starwhale import model, handler

ROOTDIR = Path(__file__).parent

@handler()
def handle(): ...

model.build(modules=[handle], workdir=ROOTDIR, name="inner")
        """
        ensure_file(workdir / "cycle_evaluator.py", content, parents=True)

        _called_build_functions.clear()

        assert len(_called_build_functions) == 0
        mock_handler = import_object(
            workdir=workdir, handler_path="cycle_evaluator:handle", py_env="venv"
        )

        build(
            modules=[mock_handler],
            workdir=workdir,
            name="outer",
        )
        assert len(_called_build_functions) == 2

        def _get_jobs_yaml(model_name: str) -> t.Dict[str, StepSpecClient]:
            path = list(
                (Path(self.local_storage) / "self" / "model" / model_name).glob(
                    "**/*.swmp/src/.starwhale/jobs.yaml"
                )
            )[0]
            return obj_to_model(load_yaml(path), JobHandlers).data

        inner_jobs = _get_jobs_yaml("inner")
        outer_jobs = _get_jobs_yaml("outer")

        assert (
            inner_jobs
            == outer_jobs
            == {
                "cycle_evaluator:handle": [
                    StepSpecClient(
                        cls_name="",
                        func_name="handle",
                        module_name="cycle_evaluator",
                        name="cycle_evaluator:handle",
                        needs=[],
                        replicas=1,
                        resources=[],
                        show_name="handle",
                        expose=0,
                        require_dataset=False,
                        parameters_sig=[],
                        ext_cmd_args="",
                    )
                ]
            }
        )

    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    @patch("starwhale.core.model.view.ModelTermView")
    def test_build_with_imported_modules(
        self, m_model_view: MagicMock, m_check_python: MagicMock
    ) -> None:
        m_check_python.return_value = [True, None, None]
        workdir = Path(self.local_storage) / "user" / "workdir"
        ensure_file(workdir / "__init__.py", "", parents=True)
        content = """
from starwhale import handler

@handler()
def handle(): ...
        """

        ensure_file(workdir / "imported_evaluator.py", content, parents=True)
        import_object(
            workdir=workdir, handler_path="imported_evaluator:handle", py_env="venv"
        )

        build(workdir=workdir)
        kwargs = m_model_view.build.call_args[1]
        assert kwargs["model_config"].name == "workdir"
        assert kwargs["model_config"].run.modules == ["imported_evaluator"]

    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    @patch("starwhale.core.model.view.ModelTermView")
    def test_build_with_workdir(
        self, m_model_view: MagicMock, m_check_python: MagicMock
    ) -> None:
        m_check_python.return_value = [True, None, None]
        workdir = Path(self.local_storage) / "user" / "workdir"
        ensure_dir(workdir / "models")
        model_fpath = workdir / "models" / "mnist_cnn.pt"
        ensure_file(model_fpath, "")
        ensure_file(workdir / "__init__.py", "")
        evaluator_fpath = workdir / "evaluator.py"
        ensure_file(evaluator_fpath, "class Handler:...")

        mock_handler = import_object(
            workdir=workdir, handler_path="evaluator:Handler", py_env="venv"
        )

        build(
            modules=[mock_handler],
            workdir=workdir,
            name="mnist",
        )

        kwargs = m_model_view.build.call_args[1]
        assert kwargs["project"] == ""
        assert kwargs["workdir"] == workdir
        assert kwargs["model_config"].run.modules == ["evaluator"]
        assert kwargs["model_config"].name == "mnist"

        sub_dir = workdir / "sub"
        ensure_dir(sub_dir)
        ensure_file(sub_dir / "__init__.py", "")
        sub_evaluator_fpath = sub_dir / "sub_evaluator.py"
        ensure_file(sub_evaluator_fpath, "class SubHandler:...")

        sub_mock_handler = import_object(
            workdir=workdir / "sub",
            handler_path="sub_evaluator:SubHandler",
            py_env="venv",
        )

        m_model_view.reset_mock()
        build(
            [sub_mock_handler, "evaluator", "sub.sub_evaluator"],
            workdir=workdir,
            name="mnist",
        )
        kwargs = m_model_view.build.call_args[1]
        assert set(kwargs["model_config"].run.modules) == set(
            [
                "sub.sub_evaluator",
                "evaluator",
            ]
        )
