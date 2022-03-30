import os
import platform
import typing as t
from pathlib import Path
import subprocess
from subprocess import check_call, check_output

from loguru import logger
import conda_pack
from fs.copy import copy_fs

from starwhale.utils import (
    get_python_run_env, get_python_version,
    is_conda, is_venv, is_darwin, is_linux, is_windows,
    get_conda_env,
)
from starwhale.utils.error import NoSupportError
from starwhale.utils.fs import ensure_dir


CONDA_ENV_TAR = "env.tar"
DUMP_CONDA_ENV_FNAME = "env-lock.yaml"
DUMP_PIP_REQ_FNAME = "pip-req-lock.txt"
DUMP_USER_PIP_REQ_FNAME = "pip-req.txt"

SUPPORTED_PIP_REQ = ["requirements.txt", "pip-req.txt", "pip3-req.txt"]


def install_req(venvdir: t.Union[str, Path], req: t.Union[str, Path]) -> None:
    #TODO: use custom pip source
    venvdir = str(venvdir)
    req = str(req)
    cmd = [os.path.join(venvdir, 'bin', 'pip'), 'install',
           '--exists-action', 'w',
           '--index-url', 'http://pypim.dapps.douban.com/simple',
           '--extra-index-url', 'https://pypi.python.org/simple/',
           '--trusted-host', 'pypim.dapps.douban.com',
           ]

    cmd += ['-r', req] if os.path.isfile(req) else [req]
    check_call(cmd)


def setup_venv(venvdir: t.Union[str, Path]) -> None:
    venvdir = str(venvdir)
    #TODO: define starwhale virtualenv.py
    check_call(['python3', '-m', 'venv', venvdir])


def pip_freeze(path: t.Union[str, Path]) -> bytes:
    #TODO: add cmd timeout and error log
    return check_output(f"pip freeze > {path}", shell=True, stderr=subprocess.STDOUT)


def conda_export(path: t.Union[str, Path], env:str="") -> bytes:
    #TODO: add cmd timeout
    cmd = "conda env export"
    env = f"-n {env}" if env else ""
    return subprocess.check_output(f"{cmd} {env} > {path}", shell=True, stderr=subprocess.STDOUT)


def dump_python_dep_env(dep_dir: t.Union[str, Path],
                        pip_req_fpath: str,
                        skip_gen_env: bool = False) -> dict:
    #TODO: smart dump python dep by starwhale sdk-api, pip ast analysis?
    dep_dir = Path(dep_dir)

    pr_env = get_python_run_env()
    sys_name = platform.system()
    py_ver = get_python_version()

    _manifest = dict(
        dep=dict(local_gen_env=False),
        env=pr_env,
        system=sys_name,
        python=py_ver,
        local_gen_env=False,
        venv=dict(use=not is_conda()),
        conda=dict(use=is_conda())
    )

    _conda_dir = dep_dir / "conda"
    _python_dir = dep_dir / "python"
    _venv_dir = _python_dir / "venv"
    _pip_lock_req = _python_dir / DUMP_PIP_REQ_FNAME
    _conda_lock_env = _conda_dir / DUMP_CONDA_ENV_FNAME

    ensure_dir(_venv_dir)
    ensure_dir(_conda_dir)
    ensure_dir(_python_dir)

    logger.info(f"[info:dep]python env({pr_env}), os({sys_name}, python({py_ver}))")

    if is_conda():
        logger.info(f"[info:dep]dump conda environment yaml: {_conda_lock_env}")
        conda_export(_conda_lock_env)
    elif is_venv():
        logger.info(f"[info:dep]dump pip-req with freeze: {_pip_lock_req}")
        pip_freeze(_pip_lock_req)
    else:
        # TODO: add other env tools
        logger.warning("detect use system python, swcli does not pip freeze, only use custom pip-req")

    if is_windows() or is_darwin() or skip_gen_env:
        #TODO: win/osx will produce env in controller agent with task
        logger.info(f"[info:dep]{sys_name} will skip conda/venv dump or generate")
    elif is_linux():
        #TODO: more design local or remote build venv
        #TODO: ignore some pkg when dump, like notebook?
        _manifest["dep"]["local_gen_env"] = True  # type: ignore

        if is_conda():
            cenv = get_conda_env()
            dest = str(_conda_dir / CONDA_ENV_TAR)
            if not cenv:
                raise Exception(f"cannot get conda env value")

            #TODO: add env/env-name into model.yaml, user can set custom vars.
            logger.info("[info:dep]try to pack conda...")
            conda_pack.pack(name=cenv, force=True, output=dest, ignore_editable_packages=True)
            logger.info(f"[info:dep]finish conda pack {dest})")
        else:
            #TODO: tune venv create performance, use clone?
            logger.info(f"[info:dep]build venv dir: {_venv_dir}")
            setup_venv(_venv_dir)
            logger.info(f"[info:dep]install pip freeze({_pip_lock_req}) to venv: {_venv_dir}")
            install_req(_venv_dir, _pip_lock_req)
            if pip_req_fpath:
                logger.info(f"[info:dep]install custom pip({pip_req_fpath}) to venv: {_venv_dir}")
                install_req(_venv_dir, pip_req_fpath)
                copy_fs(pip_req_fpath, str(_python_dir / DUMP_USER_PIP_REQ_FNAME))
    else:
        raise NoSupportError(f"no support {sys_name} system")

    return _manifest


def detect_pip_req(workdir: t.Union[str, Path], fname: str="") -> str:
    workdir = Path(workdir)

    if fname and (workdir / fname).exists():
        return str(workdir / fname)
    else:
        for p in SUPPORTED_PIP_REQ:
            if (workdir / p).exists():
                    return str(workdir / p)
            else:
                return ""