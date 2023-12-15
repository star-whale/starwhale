from __future__ import annotations

import os
import sys
import copy
import typing as t
from pathlib import Path
from functools import partial

from starwhale.utils import console
from starwhale.consts import (
    ENV_VENV,
    PythonRunEnv,
    ENV_CONDA_PREFIX,
    SWShellActivatedRuntimeEnv,
)
from starwhale.utils.fs import extract_tar
from starwhale.utils.venv import (
    get_conda_bin,
    guess_python_env_mode,
    check_valid_venv_prefix,
    check_valid_conda_prefix,
)
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.utils.process import check_call
from starwhale.core.model.model import StandaloneModel
from starwhale.base.uri.resource import Resource, ResourceType

from .model import StandaloneRuntime, RuntimeRestoreStatus


class Process:
    EnvInActivatedProcess = "SW_RUNTIME_ACTIVATED_PROCESS"
    ActivatedRuntimeURI = "SW_ACTIVATED_RUNTIME_URI_IN_SUBPROCESS"

    def __init__(
        self,
        uri: t.Union[Resource, str],
        force_restore: bool = False,
    ) -> None:
        self._uri = (
            uri
            if isinstance(uri, Resource)
            else Resource(uri, typ=ResourceType.runtime)
        )
        self._prefix_path = self._restore_runtime(force_restore=force_restore)
        self._mode = guess_python_env_mode(self._prefix_path)

    def __str__(self) -> str:
        return f"process: {self._mode} in prefix path {self._prefix_path}"

    def __repr__(self) -> str:
        return f"process: {self._mode} in prefix path {self._prefix_path}, runtime uri: {self._uri}"

    def run_self_swcli_cmd(self) -> None:
        """Run the original swcli command in the runtime environment.

        The function will remove `--runtime` option and replace the proper swcli bin path.
        """
        argv = copy.deepcopy(sys.argv)
        if len(argv) < 2:
            raise NoSupportError("no cli command")

        argv[0] = f"{self._prefix_path}/bin/swcli"

        clear_positions = []
        # support formats: "--runtime=python3.7" or "--runtime python3.7" or "-r python3.7".
        # click lib does not support "-r=python3.7" format.
        for i in range(0, len(argv)):
            if argv[i] in ("-r", "--runtime"):
                clear_positions.append(i)
                clear_positions.append(i + 1)
                break
            elif argv[i].startswith("--runtime="):
                clear_positions.append(i)
                break

        for p in clear_positions[::-1]:
            argv.pop(p)

        return self.run_arbitrary_cmd(argv)

    run = run_self_swcli_cmd

    def run_arbitrary_cmd(
        self,
        cmd: str | t.List[str],
        cwd: str | Path | None = None,
        live_stream: bool = False,
    ) -> None:
        """Run the arbitrary command in the runtime environment."""
        if os.environ.get(self.EnvInActivatedProcess, "0") == "1":
            raise RuntimeError("already in runtime activated process")

        if isinstance(cmd, (list, tuple)):
            cmd = " ".join([f"'{c}'" for c in cmd])

        # TODO: support windows platform
        if self._mode == PythonRunEnv.VENV:
            _bin_path = (self._prefix_path / "bin/activate").absolute()
            cmd = " && ".join([f"source {_bin_path}", cmd])
        elif self._mode == PythonRunEnv.CONDA:
            cmd = f"{get_conda_bin()} run --live-stream --prefix {self._prefix_path.absolute()} {cmd}"
        else:
            raise NoSupportError(f"get activate command for mode: {self._mode}")

        console.info(
            f":rooster: run process in the python isolated environment: \n"
            f"\t :herb: env prefix: {self._prefix_path} \n"
            f"\t :hibiscus: command: {cmd}"
        )
        check_call(
            ["bash", "-c", cmd],
            env={
                self.EnvInActivatedProcess: "1",
                self.ActivatedRuntimeURI: str(self._uri),
            },
            log=partial(console.print, without_timestamp=True),
            cwd=cwd,
            capture_stdout=not live_stream,
        )

    def _restore_runtime(
        self,
        force_restore: bool = False,
    ) -> Path:
        _uri = self._uri

        console.info(
            f":owl: start to run in the new process with runtime environment: {_uri}"
        )
        # TODO: support cloud runtime uri
        if _uri.instance.is_cloud:
            raise NoSupportError("run process with cloud instance uri")

        if _uri.typ == ResourceType.runtime:
            runtime = StandaloneRuntime(_uri)
            snapshot_workdir = runtime.store.snapshot_workdir
            bundle_path = runtime.store.bundle_path
        elif _uri.typ == ResourceType.model:
            model = StandaloneModel(_uri)
            snapshot_workdir = model.store.packaged_runtime_snapshot_workdir
            bundle_path = model.store.packaged_runtime_bundle_path
        else:
            raise FieldTypeOrValueError(
                f"{_uri} is not a valid uri, only support model(packaged runtime) or runtime uri"
            )

        venv_prefix = snapshot_workdir / "export" / PythonRunEnv.VENV
        conda_prefix = snapshot_workdir / "export" / PythonRunEnv.CONDA

        is_valid_prefix = check_valid_venv_prefix(
            venv_prefix
        ) or check_valid_conda_prefix(conda_prefix)

        _rrs = RuntimeRestoreStatus
        is_invalid_status = set([_rrs.get(venv_prefix), _rrs.get(conda_prefix)]) & set(
            [_rrs.failed, _rrs.restoring]
        )

        if force_restore or not is_valid_prefix or is_invalid_status:
            console.print(f":snail: start to restore runtime: {_uri}")
            extract_tar(tar_path=bundle_path, dest_dir=snapshot_workdir, force=True)
            StandaloneRuntime.restore(snapshot_workdir, verbose=False, runtime_uri=_uri)

        if venv_prefix.exists():
            prefix = venv_prefix
        elif conda_prefix.exists():
            prefix = conda_prefix
        else:
            raise RuntimeError(
                f"venv_prefix({venv_prefix}) and conda_prefix({conda_prefix}) are both not existed"
            )

        return prefix.resolve()

    @classmethod
    def get_activated_runtime_uri(cls) -> str | None:
        _env = os.environ.get
        # process activated runtime is the first priority
        uri = _env(cls.ActivatedRuntimeURI)
        if uri:
            return uri

        # detect shell activated runtime
        uri = _env(SWShellActivatedRuntimeEnv.URI)
        if not uri:
            return None

        mode = _env(SWShellActivatedRuntimeEnv.MODE)
        prefix = _env(SWShellActivatedRuntimeEnv.PREFIX)
        if not mode or not prefix:
            console.debug(
                f"env {SWShellActivatedRuntimeEnv.MODE} or {SWShellActivatedRuntimeEnv.PREFIX} is not set, skip detect"
            )
            return None

        if mode == PythonRunEnv.VENV:
            env_prefix = _env(ENV_VENV, "")
            if prefix != env_prefix:
                console.debug(
                    f"venv prefix: {prefix} is not equal to {ENV_VENV} env({env_prefix}), skip detect"
                )
                return None
            else:
                return uri
        elif mode == PythonRunEnv.CONDA:
            env_prefix = _env(ENV_CONDA_PREFIX, "")
            if prefix != env_prefix:
                console.debug(
                    f"conda prefix: {prefix} is not equal to {ENV_CONDA_PREFIX} env({env_prefix}), skip detect"
                )
                return None
            else:
                return uri
        else:
            console.debug(
                f"{SWShellActivatedRuntimeEnv.MODE}={mode} is not supported to detect shell activated runtime, skip detect"
            )
            return None
