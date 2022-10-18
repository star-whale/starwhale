from __future__ import annotations

import os
import typing as t
import tempfile
from pathlib import Path
from functools import partial

import dill

from starwhale.utils import console
from starwhale.consts import PythonRunEnv, ENV_LOG_VERBOSE_COUNT
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.utils.venv import (
    guess_python_env_mode,
    check_valid_venv_prefix,
    check_valid_conda_prefix,
)
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.process import check_call

from .model import StandaloneRuntime


class Process:

    EnvInActivatedProcess = "SW_RUNTIME_ACTIVATED_PROCESS"

    def __init__(
        self,
        prefix_path: t.Union[Path, str],
        target: t.Callable,
        args: t.Tuple = (),
        kwargs: t.Dict[str, t.Any] = {},
    ) -> None:
        self._prefix_path = Path(prefix_path).resolve()
        self._target = target
        self._args = args
        self._kwargs = kwargs
        self._mode = guess_python_env_mode(self._prefix_path)

    def __str__(self) -> str:
        return f"process: {self._target} in prefix path {self._prefix_path}"

    def __repr__(self) -> str:
        return f"process: {self._target} with args:{self._args}, kwargs:{self._kwargs} in runtime {self._prefix_path}"

    def run(self) -> None:
        partial_target = partial(self._target, *self._args, **self._kwargs)
        _, _pkl_path = tempfile.mkstemp(
            prefix="starwhale-runtime-process-", suffix=".pkl"
        )
        with open(_pkl_path, "wb") as f:
            dill.dump(partial_target, f)

        if not os.path.exists(_pkl_path):
            raise NotFoundError(f"dill file: {_pkl_path}")

        verbose = int(os.environ.get(ENV_LOG_VERBOSE_COUNT, "0"))
        try:
            cmd = [
                self._get_activate_cmd(),
                f'{self._prefix_path}/bin/python3 -c \'from starwhale.utils.debug import init_logger; init_logger({verbose}); import dill; dill.load(open("{_pkl_path}", "rb"))()\'',
            ]
            console.print(
                f":rooster: run process in the python isolated environment(prefix: {self._prefix_path})"
            )
            check_call(
                ["bash", "-c", " && ".join(cmd)],
                env={self.EnvInActivatedProcess: "1"},
                log=print,
            )
        finally:
            os.unlink(_pkl_path)

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
        force_restore: bool = False,
    ) -> Process:
        _uri: URI
        if isinstance(uri, str):
            _uri = URI(uri, expected_type=URIType.RUNTIME)
        else:
            _uri = uri
        # TODO: support cloud runtime uri
        if _uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError("run process with cloud instance uri")

        runtime = StandaloneRuntime(_uri)
        venv_prefix = runtime.store.export_dir / PythonRunEnv.VENV
        conda_prefix = runtime.store.export_dir / PythonRunEnv.CONDA
        has_restored_runtime = check_valid_venv_prefix(
            venv_prefix
        ) or check_valid_conda_prefix(conda_prefix)

        if force_restore or not has_restored_runtime:
            console.print(f":snail: start to restore runtime: {uri}")
            if not runtime.store.manifest_path.exists():
                runtime.extract(force=True)
            StandaloneRuntime.restore(runtime.store.snapshot_workdir, verbose=False)

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
