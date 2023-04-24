from __future__ import annotations

import os
import sys
import copy
import typing as t
from pathlib import Path

from starwhale.utils import console
from starwhale.consts import PythonRunEnv, DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.utils.fs import extract_tar
from starwhale.base.type import URIType, InstanceType
from starwhale.utils.venv import (
    guess_python_env_mode,
    check_valid_venv_prefix,
    check_valid_conda_prefix,
)
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.utils.process import check_call
from starwhale.core.model.model import StandaloneModel

from .model import StandaloneRuntime


class Process:
    EnvInActivatedProcess = "SW_RUNTIME_ACTIVATED_PROCESS"
    ActivatedRuntimeURI = "SW_ACTIVATED_RUNTIME_URI_IN_SUBPROCESS"

    def __init__(
        self,
        uri: t.Union[URI, str],
        force_restore: bool = False,
    ) -> None:
        self._uri = uri
        self._prefix_path = self._restore_runtime(force_restore=force_restore)
        self._mode = guess_python_env_mode(self._prefix_path)

    def __str__(self) -> str:
        return f"process: {self._mode} in prefix path {self._prefix_path}"

    def __repr__(self) -> str:
        return f"process: {self._mode} in prefix path {self._prefix_path}, runtime uri: {self._uri}"

    def run(self) -> None:
        if os.environ.get(self.EnvInActivatedProcess, "0") == "1":
            raise RuntimeError("already in runtime activated process")

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

        if not clear_positions:
            raise RuntimeError(f"no runtime specified: {argv}")

        for p in clear_positions[::-1]:
            argv.pop(p)

        cmd = [
            self._get_activate_cmd(),
            " ".join(argv),
        ]
        console.print(
            f":rooster: run process in the python isolated environment(prefix: {self._prefix_path})"
        )
        check_call(
            ["bash", "-c", " && ".join(cmd)],
            env={
                self.EnvInActivatedProcess: "1",
                self.ActivatedRuntimeURI: str(self._uri),
            },
            log=print,
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

    def _restore_runtime(
        self,
        force_restore: bool = False,
    ) -> Path:
        if isinstance(self._uri, str):
            _uri = URI.guess(self._uri, fallback_type=URIType.RUNTIME)
        else:
            _uri = self._uri

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

        return prefix.resolve()
