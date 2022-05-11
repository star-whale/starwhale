import os
import platform
import typing as t
from pathlib import Path
import shutil

from loguru import logger
import conda_pack
from rich import print as rprint
from rich.console import Console

from starwhale.utils import (
    get_python_run_env,
    get_python_version,
    is_conda,
    is_venv,
    is_darwin,
    is_linux,
    is_windows,
    get_conda_env,
)
from starwhale.utils.error import NoSupportError
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.process import check_call


CONDA_ENV_TAR = "env.tar.gz"
DUMP_CONDA_ENV_FNAME = "env-lock.yaml"
DUMP_PIP_REQ_FNAME = "pip-req-lock.txt"
DUMP_USER_PIP_REQ_FNAME = "pip-req.txt"
SW_ACTIVATE_SCRIPT = "activate.sw"

SUPPORTED_PIP_REQ = ["requirements.txt", "pip-req.txt", "pip3-req.txt"]
SW_PYPI_INDEX_URL = os.environ.get(
    "SW_PYPI_INDEX_URL", "https://pypi.doubanio.com/simple/"
)
SW_PYPI_EXTRA_INDEX_URL = os.environ.get(
    "SW_PYPI_EXTRA_INDEX_URL",
    "https://pypi.tuna.tsinghua.edu.cn/simple/ http://pypi.mirrors.ustc.edu.cn/simple/ https://pypi.org/simple",
)
SW_PYPI_TRUSTED_HOST = os.environ.get(
    "SW_PYPI_TRUSTED_HOST",
    "pypi.tuna.tsinghua.edu.cn pypi.mirrors.ustc.edu.cn pypi.doubanio.com pypi.org",
)


def install_req(venvdir: t.Union[str, Path], req: t.Union[str, Path]) -> None:
    # TODO: use custom pip source
    venvdir = str(venvdir)
    req = str(req)
    cmd = [
        os.path.join(venvdir, "bin", "pip"),
        "install",
        "--exists-action",
        "w",
        "--index-url",
        SW_PYPI_INDEX_URL,
        "--extra-index-url",
        SW_PYPI_EXTRA_INDEX_URL,
        "--trusted-host",
        SW_PYPI_TRUSTED_HOST,
    ]

    cmd += ["-r", req] if os.path.isfile(req) else [req]
    check_call(cmd)


def venv_activate(venvdir: t.Union[str, Path]) -> None:
    _fpath = Path(venvdir) / "bin" / "activate"
    cmd = f"source {_fpath.absolute()}"
    check_call(cmd, shell=True, executable="/bin/bash")


def venv_setup(venvdir: t.Union[str, Path]) -> None:
    # TODO: define starwhale virtualenv.py
    # TODO: use more elegant method to make venv portable
    check_call(f"python3 -m venv {venvdir}", shell=True)


def pip_freeze(path: t.Union[str, Path]):
    # TODO: add cmd timeout and error log
    check_call(f"pip freeze > {path}", shell=True)


def conda_export(path: t.Union[str, Path], env: str = ""):
    # TODO: add cmd timeout
    cmd = f"{get_conda_bin()} env export"
    env = f"-n {env}" if env else ""
    check_call(f"{cmd} {env} > {path}", shell=True)


def conda_restore(env_fpath: t.Union[str, Path], target_env: t.Union[str, Path]):
    cmd = f"{get_conda_bin()} env update --file {env_fpath} --prefix {target_env}"
    check_call(cmd, shell=True)


def conda_activate(env: t.Union[str, Path]) -> None:
    cmd = f"{get_conda_bin()} activate {env}"
    check_call(cmd, shell=True)


def conda_activate_render(env: t.Union[str, Path], path: Path) -> None:
    content = """
_conda_hook="$(/opt/miniconda3/bin/conda shell.bash hook)"
cat >> /dev/stdout << EOF
$_conda_hook
conda activate /opt/starwhale/swmp/dep/conda/env
EOF
"""
    _render_sw_activate(content, path)


def venv_activate_render(
    venvdir: t.Union[str, Path], path: Path, relocate: bool = False
) -> None:
    bin = f"{venvdir}/bin"
    if relocate:
        content = f"""
sed -i '1d' {bin}/starwhale {bin}/sw {bin}/swcli {bin}/pip* {bin}/virtualenv
sed -i '1i\#!{bin}/python3' {bin}/starwhale {bin}/sw {bin}/swcli {bin}/pip* {bin}/virtualenv

sed -i 's#^VIRTUAL_ENV=.*$#VIRTUAL_ENV={venvdir}#g' {bin}/activate
rm -rf {bin}/python3
ln -s /usr/bin/python3 {bin}/python3
echo 'source {bin}/activate'
"""
    else:
        content = f"""
echo 'source {venvdir}/bin/activate'
"""
    _render_sw_activate(content, path)


def _render_sw_activate(content: str, path: Path) -> None:
    ensure_file(path, content, mode=0o755)
    rprint(f" :clap: {path.name} is generated at {path}")
    rprint(" :compass: run cmd:  ")
    rprint(f" \t [bold red] $(sh {path}) [/]")


def get_conda_bin() -> str:
    # TODO: add process cache
    for _p in (
        "/opt/miniconda3/bin/conda",
        "/opt/anaconda3/bin/conda",
        os.path.expanduser("~/miniconda3/bin/conda"),
        os.path.expanduser("~/anaconda3/bin/conda"),
    ):
        if os.path.exists(_p):
            return _p
    else:
        return "conda"


def dump_python_dep_env(
    dep_dir: t.Union[str, Path],
    pip_req_fpath: str,
    skip_gen_env: bool = False,
    console: t.Optional[Console] = None,
) -> dict:
    # TODO: smart dump python dep by starwhale sdk-api, pip ast analysis?
    dep_dir = Path(dep_dir)
    console = console or Console()

    pr_env = get_python_run_env()
    sys_name = platform.system()
    py_ver = get_python_version()

    _manifest = dict(
        env=pr_env,
        system=sys_name,
        python=py_ver,
        local_gen_env=False,
        venv=dict(use=not is_conda()),
        conda=dict(use=is_conda()),
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
    console.print(f":dizzy: python{py_ver}@{pr_env}, try to export environment...")

    if os.path.exists(pip_req_fpath):
        shutil.copyfile(pip_req_fpath, str(_python_dir / DUMP_USER_PIP_REQ_FNAME))

    if is_conda():
        logger.info(f"[info:dep]dump conda environment yaml: {_conda_lock_env}")
        conda_export(_conda_lock_env)
    elif is_venv():
        logger.info(f"[info:dep]dump pip-req with freeze: {_pip_lock_req}")
        pip_freeze(_pip_lock_req)
    else:
        # TODO: add other env tools
        logger.warning(
            "detect use system python, swcli does not pip freeze, only use custom pip-req"
        )

    if is_windows() or is_darwin() or skip_gen_env:
        # TODO: win/osx will produce env in controller agent with task
        logger.info(f"[info:dep]{sys_name} will skip conda/venv dump or generate")
    elif is_linux():
        # TODO: more design local or remote build venv
        # TODO: ignore some pkg when dump, like notebook?
        _manifest["local_gen_env"] = True  # type: ignore

        if is_conda():
            cenv = get_conda_env()
            dest = str(_conda_dir / CONDA_ENV_TAR)
            if not cenv:
                raise Exception("cannot get conda env value")

            # TODO: add env/env-name into model.yaml, user can set custom vars.
            logger.info("[info:dep]try to pack conda...")
            conda_pack.pack(
                name=cenv, force=True, output=dest, ignore_editable_packages=True
            )
            logger.info(f"[info:dep]finish conda pack {dest})")
            console.print(f":beer_mug: conda pack @ [underline]{dest}[/]")
        else:
            # TODO: tune venv create performance, use clone?
            logger.info(f"[info:dep]build venv dir: {_venv_dir}")
            venv_setup(_venv_dir)
            logger.info(
                f"[info:dep]install pip freeze({_pip_lock_req}) to venv: {_venv_dir}"
            )
            install_req(_venv_dir, _pip_lock_req)
            if os.path.exists(pip_req_fpath):
                logger.info(
                    f"[info:dep]install custom pip({pip_req_fpath}) to venv: {_venv_dir}"
                )
                install_req(_venv_dir, pip_req_fpath)
            console.print(f":beer_mug: venv @ [underline]{_venv_dir}[/]")

    else:
        raise NoSupportError(f"no support {sys_name} system")

    return _manifest


def detect_pip_req(workdir: t.Union[str, Path], fname: str = "") -> str:
    workdir = Path(workdir)

    if fname and (workdir / fname).exists():
        return str(workdir / fname)
    else:
        for p in SUPPORTED_PIP_REQ:
            if (workdir / p).exists():
                return str(workdir / p)
            else:
                return ""
