import os
from pathlib import Path
from unittest.mock import patch, MagicMock

from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.load import import_object
from starwhale.api._impl.model import build
from starwhale.core.model.model import ModelConfig

from .. import BaseTestCase


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
                ModelBuildTestCase,
                workdir="not_found",
            )

        with self.assertRaisesRegex(TypeError, "is a built-in class"):
            build(evaluation_handler=list)

    @patch("os.unlink")
    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    @patch("starwhale.core.model.view.ModelTermView")
    def test_build_with_cwd(
        self, m_model_view: MagicMock, m_check_python: MagicMock, m_unlink: MagicMock
    ) -> None:
        m_check_python.return_value = [True, None, None]
        workdir = Path(self.local_storage) / "user" / "workdir"
        ensure_dir(workdir)
        ensure_file(workdir / "cwd_evaluator.py", "class Handler:...")

        mock_handler = import_object(
            workdir=workdir, handler_path="cwd_evaluator:Handler", py_env="venv"
        )
        os.chdir(workdir)
        build(mock_handler)

        assert m_unlink.call_count == 1
        model_yaml_fpath = m_unlink.call_args[0][0]
        model_config = ModelConfig.create_by_yaml(model_yaml_fpath)
        assert model_config.name == "workdir"
        assert model_config.run.handler == "cwd_evaluator:Handler"
        assert (
            m_model_view.build.call_args[1]["workdir"]
        ).resolve().absolute() == workdir.resolve().absolute()

    @patch("os.unlink")
    @patch("starwhale.utils.load.check_python_interpreter_consistency")
    @patch("starwhale.core.model.view.ModelTermView")
    def test_build_with_workdir(
        self, m_model_view: MagicMock, m_check_python: MagicMock, m_unlink: MagicMock
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
            mock_handler,
            workdir=workdir,
            name="mnist",
        )

        assert m_unlink.call_count == 1
        model_yaml_fpath = m_unlink.call_args[0][0]
        model_config = ModelConfig.create_by_yaml(model_yaml_fpath)
        assert model_config.name == "mnist"
        assert model_config.run.handler == "evaluator:Handler"

        kwargs = m_model_view.build.call_args[1]
        assert kwargs["project"] == ""
        assert kwargs["yaml_path"] == model_yaml_fpath
        assert kwargs["workdir"] == workdir

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
            sub_mock_handler,
            workdir=workdir,
            name="mnist",
        )
        model_yaml_fpath = m_unlink.call_args[0][0]
        model_config = ModelConfig.create_by_yaml(model_yaml_fpath)
        assert model_config.run.handler == "sub.sub_evaluator:SubHandler"
