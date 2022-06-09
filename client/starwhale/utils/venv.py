import os
import shutil
import typing as t
import tarfile
import platform
import subprocess
from pathlib import Path

import conda_pack
import virtualenv
from loguru import logger

from starwhale.utils import console, is_linux, is_darwin, is_windows, get_python_version
from starwhale.consts import (
    ENV_VENV,
    ENV_CONDA,
    PythonRunEnv,
    ENV_CONDA_PREFIX,
    DEFAULT_PYTHON_VERSION,
)
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.utils.error import (
    FormatError,
    ExistedError,
    NoSupportError,
    UnExpectedConfigFieldError,
)
from starwhale.utils.process import check_call

CONDA_ENV_TAR = "env.tar.gz"
DUMP_CONDA_ENV_FNAME = "env-lock.yaml"
DUMP_PIP_REQ_FNAME = "requirements-lock.txt"
DUMP_USER_PIP_REQ_FNAME = "requirements.txt"
SW_ACTIVATE_SCRIPT = "activate.sw"

SUPPORTED_PIP_REQ = [DUMP_USER_PIP_REQ_FNAME, "pip-req.txt", "pip3-req.txt"]
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
_DUMMY_FIELD = -1


class PythonVersionField(t.NamedTuple):
    major: int = _DUMMY_FIELD
    minor: int = _DUMMY_FIELD
    micro: int = _DUMMY_FIELD


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


def parse_python_version(s: str) -> PythonVersionField:
    s = s.strip().lower()

    if not s:
        raise FormatError("python version empty")

    if s.startswith("python"):
        s = s.split("python", 1)[-1]

    _vt = s.split(".")
    _major, _minor, _micro = int(_vt[0]), _DUMMY_FIELD, _DUMMY_FIELD

    if len(_vt) >= 2:
        _minor = int(_vt[1])

    if len(_vt) >= 3:
        _micro = int(_vt[2])

    return PythonVersionField(major=_major, minor=_minor, micro=_micro)


def venv_setup(
    venvdir: t.Union[str, Path],
    python_version: str = "",
    prompt: str = "",
    clear: bool = False,
) -> None:
    # TODO: define starwhale virtualenv.py
    # TODO: use more elegant method to make venv portable
    args = [str(venvdir)]
    if prompt:
        args += ["--prompt", prompt]

    if python_version:
        args += ["--python", python_version]

    if clear:
        args += ["--clear"]

    session = virtualenv.cli_run(args)
    console.print(f":clap: create venv@{venvdir}, python:{session.interpreter.version}")  # type: ignore


def pip_freeze(path: t.Union[str, Path], include_editable: bool = False) -> None:
    # TODO: add cmd timeout and error log
    cmd = ["pip", "freeze", "--require-virtualenv"]
    if not include_editable:
        cmd += ["--exclude-editable"]
    cmd += [">", str(path)]

    check_call(" ".join(cmd), shell=True)


def conda_create(
    env: str,
    python_version: str = DEFAULT_PYTHON_VERSION,
    quiet: bool = False,
) -> None:
    cmd = ["conda", "create", "--name", env, "--yes"]

    if quiet:
        cmd += ["--quiet"]

    cmd += [f"python={python_version}"]
    check_call(cmd)


def conda_export(path: t.Union[str, Path], env: str = "") -> None:
    # TODO: add cmd timeout
    cmd = f"{get_conda_bin()} env export"
    env = f"-n {env}" if env else ""
    check_call(f"{cmd} {env} > {path}", shell=True)


def get_external_python_version() -> str:
    out = subprocess.check_output(
        [
            "python3",
            "-c",
            "import sys; _v = sys.version_info; print(f'{_v,major}.{_v.minor}.{_v.micro}')",
        ],
    )
    return out.decode().strip()


def get_pip_cache_dir() -> str:
    out = subprocess.check_output(
        ["python3", "-m", "pip", "cache", "dir"],
    )
    return out.decode().strip()


def conda_restore(
    env_fpath: t.Union[str, Path], target_env: t.Union[str, Path]
) -> None:
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
        # TODO: support relocatable editable python package
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
    console.print(f" :clap: {path.name} is generated at {path}")
    console.print(" :compass: run cmd:  ")
    console.print(f" \t [bold red] $(sh {path}) [/]")


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
    gen_all_bundles: bool = False,
    expected_runtime: str = "",
    mode: str = PythonRunEnv.AUTO,
    include_editable: bool = False,
) -> t.Dict[str, t.Any]:
    # TODO: smart dump python dep by starwhale sdk-api, pip ast analysis?
    dep_dir = Path(dep_dir)

    pr_env = get_python_run_env(mode)
    sys_name = platform.system()
    py_ver = get_python_version()

    expected_runtime = expected_runtime.strip().lower()
    if expected_runtime and not py_ver.startswith(expected_runtime):
        raise UnExpectedConfigFieldError(
            f"expected runtime({expected_runtime}) is not equal to detected runtime{py_ver}"
        )

    _manifest = dict(
        expected_mode=mode,
        env=pr_env,
        system=sys_name,
        python=py_ver,
        local_gen_env=False,
        venv=dict(use=is_venv()),
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

    console.print(
        f":dizzy: python{py_ver}@{pr_env}, os({sys_name}), include-editable({include_editable}), try to export environment..."
    )

    if os.path.exists(pip_req_fpath):
        shutil.copyfile(pip_req_fpath, str(_python_dir / DUMP_USER_PIP_REQ_FNAME))

    if pr_env == PythonRunEnv.CONDA:
        if include_editable:
            raise NoSupportError("conda cannot support export pip editable package")

        logger.info(f"[info:dep]dump conda environment yaml: {_conda_lock_env}")
        conda_export(_conda_lock_env)
    elif pr_env == PythonRunEnv.VENV:
        logger.info(f"[info:dep]dump pip-req with freeze: {_pip_lock_req}")
        pip_freeze(_pip_lock_req, include_editable)
    else:
        # TODO: add other env tools
        logger.warning(
            "detect use system python, swcli does not pip freeze, only use custom pip-req"
        )

    if is_windows() or is_darwin() or not gen_all_bundles:
        # TODO: win/osx will produce env in controller agent with task
        logger.info(f"[info:dep]{sys_name} will skip conda/venv dump or generate")
    elif is_linux():
        # TODO: more design local or remote build venv
        # TODO: ignore some pkg when dump, like notebook?
        _manifest["local_gen_env"] = True  # type: ignore

        if pr_env == PythonRunEnv.CONDA:
            cenv = get_conda_env()
            dest = str(_conda_dir / CONDA_ENV_TAR)
            if not cenv:
                raise Exception("cannot get conda env value")

            # TODO: add env/env-name into model.yaml, user can set custom vars.
            logger.info("[info:dep]try to pack conda...")
            conda_pack.pack(
                name=cenv,
                force=True,
                output=dest,
                ignore_editable_packages=not include_editable,
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
                # TODO: support ignore editable package
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
    return ""


def activate_python_env(
    mode: str,
    identity: str,
) -> None:
    # TODO: switch shell python environment directly
    console.print(":cake: run command in shell :cake:")

    if mode == PythonRunEnv.VENV:
        cmd = f"source {identity}/bin/activate"
    elif mode == PythonRunEnv.CONDA:
        cmd = f"conda activate {identity}"
    else:
        raise NoSupportError(mode)

    console.print(f"\t[red][blod]{cmd}")


def create_python_env(
    mode: str,
    name: str,
    workdir: Path,
    python_version: str = DEFAULT_PYTHON_VERSION,
    force: bool = False,
) -> str:

    if mode == PythonRunEnv.VENV:
        venvdir = workdir / "venv"
        if venvdir.exists() and not force:
            raise ExistedError(str(venvdir))

        logger.info(f"create venv @ {venvdir}...")
        venv_setup(venvdir, python_version=python_version, prompt=name)
        return str(venvdir.absolute())
    elif mode == PythonRunEnv.CONDA:
        logger.info(f"create conda {name}:{workdir}, use python {python_version}...")
        conda_create(name, python_version)
        return name
    else:
        raise NoSupportError(mode)


def is_venv() -> bool:
    # TODO: refactor for get external venv attr
    output = subprocess.check_output(
        [
            "python3",
            "-c",
            "import sys; print(sys.prefix != (getattr(sys, 'base_prefix', None) or (getattr(sys, 'real_prefix', None) or sys.prefix)))",  # noqa: E501
        ],
    )
    return "True" in str(output) or get_venv_env() != ""


def get_venv_env() -> str:
    return os.environ.get(ENV_VENV, "")


def is_conda() -> bool:
    return get_conda_env() != "" and get_conda_env_prefix() != ""


def get_python_run_env(mode: str = PythonRunEnv.AUTO) -> str:
    if mode == PythonRunEnv.VENV:
        if is_venv():
            return PythonRunEnv.VENV
        else:
            raise EnvironmentError(
                "expected venv mode, but cannot find venv environment"
            )
    elif mode == PythonRunEnv.CONDA:
        if is_conda():
            return PythonRunEnv.CONDA
        else:
            raise EnvironmentError("expected conda mmode, but cannot find conda envs")
    elif mode == PythonRunEnv.AUTO:
        if is_conda() and is_venv():
            raise EnvironmentError("find venv and conda both activate")

        if is_conda():
            return PythonRunEnv.CONDA
        elif is_venv():
            return PythonRunEnv.VENV
        else:
            return PythonRunEnv.SYSTEM
    else:
        raise NoSupportError(f"python run env: {mode}")


def get_conda_env() -> str:
    return os.environ.get(ENV_CONDA, "")


def get_conda_env_prefix() -> str:
    return os.environ.get(ENV_CONDA_PREFIX, "")


def restore_python_env(
    _workdir: Path, _mode: str, _local_gen_env: bool = False
) -> None:
    console.print(
        f":bread: restore python {_mode} @ {_workdir}, use local env data: {_local_gen_env}"
    )
    _f = _do_restore_conda if _mode == PythonRunEnv.CONDA else _do_restore_venv
    _f(_workdir, _local_gen_env)


def _do_restore_conda(_workdir: Path, _local_gen_env: bool) -> None:
    _ascript = _workdir / SW_ACTIVATE_SCRIPT
    _conda_dir = _workdir / "dep" / "conda"
    _tar_fpath = _conda_dir / CONDA_ENV_TAR
    _env_dir = _conda_dir / "env"

    if _local_gen_env and _tar_fpath.exists():
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


def _do_restore_venv(
    _workdir: Path, _local_gen_env: bool, _rebuild: bool = False
) -> None:
    _ascript = _workdir / SW_ACTIVATE_SCRIPT
    _python_dir = _workdir / "dep" / "python"
    _venv_dir = _python_dir / "venv"

    _relocate = True
    if _rebuild or not _local_gen_env or not (_venv_dir / "bin" / "activate").exists():
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
