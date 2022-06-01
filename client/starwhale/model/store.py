from http import HTTPStatus
from pathlib import Path
import yaml
import sys
import typing as t
import tarfile

import click
import requests
from rich.panel import Panel
from rich.pretty import Pretty
from rich import print as rprint
from fs import open_fs
from fs.tarfs import TarFS
from loguru import logger

from starwhale.utils import fmt_http_server, pretty_bytes
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME,
    DEFAULT_MODEL_YAML_NAME,
    SW_API_VERSION,
)
from starwhale.utils.venv import (
    CONDA_ENV_TAR,
    DUMP_CONDA_ENV_FNAME,
    DUMP_PIP_REQ_FNAME,
    DUMP_USER_PIP_REQ_FNAME,
    install_req,
    venv_activate_render,
    conda_activate_render,
    conda_restore,
    venv_setup,
    SW_ACTIVATE_SCRIPT,
)
from starwhale.utils.fs import ensure_dir, empty_dir
from starwhale.base.store import LocalStorage
from starwhale.utils.error import NotFoundError
from starwhale.utils.http import wrap_sw_error_resp, upload_file

TMP_FILE_BUFSIZE = 8192
_SWMP_FILE_TYPE = ".swmp"


class ModelPackageLocalStore(LocalStorage):
    def list(
        self,
        filter: str = "",
        title: str = "",
        caption: str = "",
        fullname: bool = False,
    ) -> None:
        super().list(
            filter=filter,
            title="List swmp in local storage",
            caption=f"@{self.pkgdir}",
            fullname=fullname,
        )

    def iter_local_swobj(self) -> t.Generator[LocalStorage.SWobjMeta, None, None]:
        if not self.pkgdir.exists():
            return

        pkg_fs = open_fs(str(self.pkgdir.resolve()))

        for mdir in pkg_fs.scandir("."):
            if not mdir.is_dir:
                continue

            for _fname in pkg_fs.opendir(mdir.name).listdir("."):
                if _fname != self.LATEST_TAG and not _fname.endswith(_SWMP_FILE_TYPE):
                    continue

                _path = self.pkgdir / mdir.name / _fname
                _manifest = self._load_swmp_manifest(_path.resolve())
                _tag = _fname if _fname == self.LATEST_TAG else ""

                yield LocalStorage.SWobjMeta(
                    name=mdir.name,
                    version=_manifest["version"],
                    tag=_tag,
                    environment=_manifest["dep"]["env"],
                    size=pretty_bytes(_path.stat().st_size),
                    generate="local" if _manifest["dep"]["local_gen_env"] else "remote",
                    created=_manifest["created_at"],
                )

    def _load_swmp_manifest(self, fpath: Path, direct: bool = False) -> t.Any:
        if not direct and fpath.name.endswith(_SWMP_FILE_TYPE):
            _mname = fpath.parent.name
            _mversion = fpath.name.split(_SWMP_FILE_TYPE)[0]
            _extracted_fpath = self.workdir / _mname / _mversion / DEFAULT_MANIFEST_NAME
            if _extracted_fpath.exists():
                return yaml.safe_load(_extracted_fpath.open())

        with TarFS(str(fpath)) as tar:
            return yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))

    def push(self, swmp: str, project: str = "", force: bool = False) -> None:
        # TODO: add more restful api for project, /api/v1/project/{project_id}/model/push
        url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/project/model/push"

        _spath, _full_swmp = self._get_swmp_path(swmp)
        if not _spath.exists():
            rprint(
                f"[red]failed to push {_full_swmp}[/], because of {_spath} not found"
            )
            sys.exit(1)

        rprint(f":fire: try to push swmp({_full_swmp})...")
        upload_file(
            url=url,
            fpath=_spath,
            fields={
                "swmp": _full_swmp,
                "project": project,
                "force": "1" if force else "0",
            },
            headers={"Authorization": self._sw_token},
            exit=True,
        )
        rprint(" :clap: push done.")

    def pull(
        self, swmp: str, project: str = "", server: str = "", force: bool = False
    ) -> None:
        server = server.strip() or self.sw_remote_addr
        server = fmt_http_server(server)
        url = f"{server}/api/{SW_API_VERSION}/project/model/pull"

        _spath, _ = self._get_swmp_path(swmp)
        if _spath.exists() and not force:
            rprint(f":ghost: {swmp} is already existed, skip pull")
            return

        # TODO: add progress bar and rich live
        # TODO: multi phase for pull swmp
        # TODO: get size in advance
        rprint(f"try to pull {swmp}")
        with requests.get(
            url,
            stream=True,
            params={"swmp": swmp, "project": project},
            headers={"Authorization": self._sw_token},
        ) as r:
            if r.status_code == HTTPStatus.OK:
                with _spath.open("wb") as f:
                    for chunk in r.iter_content(chunk_size=TMP_FILE_BUFSIZE):
                        f.write(chunk)
                rprint(":clap: pull completed")
            else:
                wrap_sw_error_resp(r, "pull failed", exit=True)

    def info(self, swmp: str) -> None:
        _manifest = self.get_swmp_info(*self._parse_swobj(swmp))
        _config_panel = Panel(
            Pretty(_manifest, expand_all=True),
            title="inspect _manifest.yaml / model.yaml info",
        )
        self._console.print(_config_panel)
        # TODO: add workdir tree

    def get_swmp_info(self, _name: str, _version: str) -> t.Dict[str, t.Any]:
        _workdir, _ = self._guess(self.workdir / _name, _version)
        _swmp_path, _ = self._guess(self.pkgdir / _name, _version)

        _manifest: t.Dict[str, t.Any] = {}
        if _workdir.exists():
            _manifest = yaml.safe_load((_workdir / DEFAULT_MANIFEST_NAME).open())
            _model = yaml.safe_load((_workdir / DEFAULT_MODEL_YAML_NAME).open())
        elif _swmp_path.exists():
            with TarFS(str(_swmp_path.resolve())) as tar:
                _manifest = yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))
                _model = yaml.safe_load(tar.open(DEFAULT_MODEL_YAML_NAME))
        else:
            raise NotFoundError(f"{_workdir} and {_swmp_path} are both not existed.")

        _manifest.update(_model)
        _manifest["workdir"] = str(_workdir.resolve())
        _manifest["pkg"] = str(_swmp_path.resolve())
        return _manifest

    def gc(self, dry_run: bool = False) -> None:
        ...

    def delete(self, swmp: str) -> None:
        _model, _version = self._parse_swobj(swmp)

        def _remove_workdir(_real_version: str) -> None:
            workdir_fpath, _ = self._guess(self.workdir / _model, _real_version)
            if not (workdir_fpath.exists() and workdir_fpath.is_dir()):
                return

            click.confirm(f"continue to delete {workdir_fpath}?", abort=True)
            empty_dir(workdir_fpath)
            rprint(f" :bomb: delete workdir {workdir_fpath}")

        pkg_fpath, _ = self._guess(self.pkgdir / _model, _version)
        if pkg_fpath.exists():
            click.confirm(f"continue to delete {pkg_fpath}?", abort=True)
            pkg_fpath = pkg_fpath.resolve()
            pkg_fpath.unlink()
            rprint(f" :collision: delete swmp {pkg_fpath}")

            _remove_workdir(pkg_fpath.name)

            latest = self.pkgdir / _model / self.LATEST_TAG
            if _version == self.LATEST_TAG or latest.resolve() == pkg_fpath:
                latest.unlink()
                rprint(f" :bomb: delete swmp {latest}")

        _remove_workdir(_version)

    def extract(
        self, swmp: str, force: bool = False, _target: t.Optional[Path] = None
    ) -> Path:
        _name, _version = self._parse_swobj(swmp)
        if _target:
            _target = Path(_target) / _version
        else:
            _target, _ = self._guess(self.workdir / _name, _version)

        if (
            _target.exists()
            and (_target / DEFAULT_MANIFEST_NAME).exists()
            and not force
        ):
            self._console.print(f":joy_cat: {swmp} existed, skip extract swmp")
        else:
            empty_dir(_target)
            ensure_dir(_target)
            self._console.print(":oncoming_police_car: try to extract swmp...")
            _swmp_path, _ = self._get_swmp_path(swmp)
            with tarfile.open(_swmp_path, "r") as tar:
                tar.extractall(path=str(_target.resolve()))

        if not (_target / DEFAULT_MANIFEST_NAME).exists():
            raise Exception("invalid swmp model dir")

        self._console.print(f":clap: extracted-swmp @ {_target.resolve()}")
        return _target

    def _get_swmp_path(self, swmp: str) -> t.Tuple[Path, str]:
        _model, _version = self._parse_swobj(swmp)
        _dir, _fullversion = self._guess(
            self.pkgdir / _model, _version, ftype=_SWMP_FILE_TYPE
        )
        return _dir, f"{_model}:{_fullversion}"

    def pre_activate(self, swmp: str) -> None:
        if swmp.count(":") == 1:
            _name, _version = self._parse_swobj(swmp)
            _workdir, _ = self._guess(self.workdir / _name, _version)
        else:
            _workdir = Path(swmp)

        _workdir = _workdir.resolve()
        _manifest = yaml.safe_load((_workdir / DEFAULT_MANIFEST_NAME).open())

        _env = _manifest["dep"]["env"]
        _f = getattr(self, f"_activate_{_env}")
        _f(_workdir, _manifest["dep"])

    def _activate_conda(self, _workdir: Path, _dep: t.Dict[str, t.Any]) -> None:
        if not _dep["conda"]["use"]:
            raise Exception("env set conda, but conda:use is false")

        _ascript = _workdir / SW_ACTIVATE_SCRIPT
        _conda_dir = _workdir / "dep" / "conda"
        _tar_fpath = _conda_dir / CONDA_ENV_TAR
        _env_dir = _conda_dir / "env"

        if _dep["local_gen_env"] and _tar_fpath.exists():
            empty_dir(_env_dir)
            ensure_dir(_env_dir)
            logger.info(f"extract {_tar_fpath} ...")
            with tarfile.open(str(_tar_fpath)) as f:
                f.extractall(str(_env_dir))

            logger.info(f"render activate script: {_ascript}")
            venv_activate_render(_env_dir, _ascript)
        else:
            logger.info("restore conda env ...")
            _env_yaml = _conda_dir / DUMP_CONDA_ENV_FNAME
            # TODO: controller will proceed in advance
            conda_restore(_env_yaml, _env_dir)

            logger.info(f"render activate script: {_ascript}")
            conda_activate_render(_env_dir, _ascript)

    def _activate_venv(
        self, _workdir: Path, _dep: t.Dict[str, t.Any], _rebuild: bool = False
    ) -> None:
        if not _dep["venv"]["use"] and not _rebuild:
            raise Exception("env set venv, but venv:use is false")

        _ascript = _workdir / SW_ACTIVATE_SCRIPT
        _python_dir = _workdir / "dep" / "python"
        _venv_dir = _python_dir / "venv"

        _relocate = True
        if (
            _rebuild
            or not _dep["local_gen_env"]
            or not (_venv_dir / "bin" / "activate").exists()
        ):
            logger.info(f"setup venv and pip install {_venv_dir}")
            _relocate = False
            venv_setup(_venv_dir)
            for _name in (DUMP_PIP_REQ_FNAME, DUMP_USER_PIP_REQ_FNAME):
                _path = _python_dir / _name
                if not _path.exists():
                    continue

                logger.info(f"pip install {_path} ...")
                install_req(_venv_dir, _path)

        logger.info(f"render activate script: {_ascript}")
        venv_activate_render(_venv_dir, _ascript, relocate=_relocate)

    def _activate_system(self, _workdir: Path, _dep: t.Dict[str, t.Any]) -> None:
        self._activate_venv(_workdir, _dep, _rebuild=True)
