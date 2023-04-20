from __future__ import annotations

import os
import typing as t
import tempfile
from pathlib import Path
from functools import partial

import dill

from starwhale.utils import console
from starwhale.consts import PythonRunEnv, DEFAULT_MANIFEST_NAME, ENV_LOG_VERBOSE_COUNT
from starwhale.base.uri import URI
from starwhale.utils.fs import extract_tar
from starwhale.base.type import URIType, InstanceType
from starwhale.utils.venv import (
    guess_python_env_mode,
    check_valid_venv_prefix,
    check_valid_conda_prefix,
)
from starwhale.utils.error import NotFoundError, NoSupportError, FieldTypeOrValueError
from starwhale.utils.process import check_call
from starwhale.core.model.model import StandaloneModel

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
            _uri = URI.guess(uri, fallback_type=URIType.RUNTIME)
        else:
            _uri = uri

        console.print(
            f":owl: start to run in the new process with runtime environment: {_uri}"
        )
        # TODO: support cloud runtime uri
        if _uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError("run process with cloud instance uri")

        if _uri.object.typ == URIType.RUNTIME:
            runtime = StandaloneRuntime(_uri)
            snapshot_workdir = runtime.store.snapshot_workdir
            bundle_path = runtime.store.bundle_path
        elif _uri.object.typ == URIType.MODEL:
            model = StandaloneModel(_uri)
            snapshot_workdir = model.store.packaged_runtime_snapshot_workdir
            bundle_path = model.store.packaged_runtime_bundle_path
        else:
            raise FieldTypeOrValueError(
                f"{_uri} is not a valid uri, only support model(packaged runtime) or runtime uri"
            )

        venv_prefix = snapshot_workdir / "export" / PythonRunEnv.VENV
        conda_prefix = snapshot_workdir / "export" / PythonRunEnv.CONDA
        has_restored_runtime = check_valid_venv_prefix(
            venv_prefix
        ) or check_valid_conda_prefix(conda_prefix)

        if force_restore or not has_restored_runtime:
            console.print(f":snail: start to restore runtime: {_uri}")

            if not (snapshot_workdir / DEFAULT_MANIFEST_NAME).exists():
                extract_tar(
                    tar_path=bundle_path,
                    dest_dir=snapshot_workdir,
                    force=True,
                )

            StandaloneRuntime.restore(snapshot_workdir, verbose=False)

        if venv_prefix.exists():
            prefix = venv_prefix
        elif conda_prefix.exists():
            prefix = conda_prefix
        else:
            raise RuntimeError(
                f"venv_prefix({venv_prefix}) and conda_prefix({conda_prefix}) are both not existed"
            )

        return cls(
            prefix_path=prefix,
            target=target,
            args=args,
            kwargs=kwargs,
        )
