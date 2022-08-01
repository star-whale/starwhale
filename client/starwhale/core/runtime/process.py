from __future__ import annotations

import typing as t
import tempfile
from pathlib import Path
from functools import partial

import dill

from starwhale.consts import PythonRunEnv
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.utils.venv import guess_python_env_mode
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.process import check_call

from .model import StandaloneRuntime


class Process:

    EnvInActivatedProcess = "SW_RUNTIME_ACTIVATED_PROCESS"

    def __init__(
        self,
        prefix_path: Path,
        target: t.Callable,
        args: t.Tuple = (),
        kwargs: t.Dict[str, t.Any] = {},
    ) -> None:
        self._prefix_path = prefix_path
        self._target = target
        self._args = args
        self._kwargs = kwargs
        self._mode = guess_python_env_mode(prefix_path)

    def __str__(self) -> str:
        return f"process: {self._target} in prefix path {self._prefix_path}"

    def __repr__(self) -> str:
        return f"process: {self._target} with args:{self._args}, kwargs:{self._kwargs} in runtime {self._prefix_path}"

    def run(self) -> None:
        _, pkl_path = tempfile.mkstemp(
            prefix="starwhale-runtime-process-", suffix="pkl"
        )
        partial_target = partial(self._target, *self._args, **self._kwargs)
        dill.dump(partial_target, pkl_path)

        cmd = [
            self._get_activate_cmd(),
            f"python3 -c 'import dill; dill.load(\"{pkl_path}\")()'",
        ]
        check_call(
            ["bash", "-c", " && ".join(cmd)], env={self.EnvInActivatedProcess: "1"}
        )

    def _get_activate_cmd(self) -> str:
        # TODO: support windows platform
        if self._mode == PythonRunEnv.VENV:
            _ap = self._prefix_path / "bin/activate"
            return f"source {_ap.absolute()}"
        elif self._mode == PythonRunEnv.CONDA:
            return f"source activate {self._prefix_path.absolute()}"
        else:
            raise NoSupportError(f"get activate command for mode: {self._mode}")

    @classmethod
    def from_runtime_uri(
        cls,
        uri: t.Union[str, URI],
        target: t.Callable,
        args: t.Tuple = (),
        kwargs: t.Dict[str, t.Any] = {},
    ) -> Process:
        _uri: URI
        if isinstance(uri, str):
            _uri = URI(uri, expected_type=URIType.RUNTIME)
        else:
            _uri = uri
        # TODO: support cloud runtime uri
        # TODO: auto extract and restore runtime uri
        if _uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError("run process with cloud instance uri")

        runtime = StandaloneRuntime(_uri)
        venv_prefix = runtime.store.export_dir / PythonRunEnv.VENV
        conda_prefix = runtime.store.export_dir / PythonRunEnv.CONDA

        prefix = Path()
        if venv_prefix.exists():
            prefix = venv_prefix
        elif conda_prefix.exists():
            prefix = conda_prefix
        else:
            raise NotFoundError(f"{runtime.store.export_dir} cannot find valid env dir")

        return cls(
            prefix_path=prefix,
            target=target,
            args=args,
            kwargs=kwargs,
        )
