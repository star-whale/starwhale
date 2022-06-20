import os
import sys
import shutil
import typing as t
import tarfile
import platform
import subprocess
from pathlib import Path

import conda_pack
import virtualenv
from loguru import logger

from starwhale.utils import console, is_linux, is_darwin, is_windows
from starwhale.consts import (
    ENV_VENV,
    ENV_CONDA,
    PythonRunEnv,
    ENV_CONDA_PREFIX,
    SW_PYPI_PKG_NAME,
    DEFAULT_PYTHON_VERSION,
)
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.utils.error import (
    FormatError,
    ExistedError,
    NotFoundError,
    NoSupportError,
    PythonEnvironmentError,
)
from starwhale.utils.process import check_call

CONDA_ENV_TAR = "env.tar.gz"
DUMP_CONDA_ENV_FNAME = "env-lock.yaml"
DUMP_PIP_REQ_FNAME = "requirements-lock.txt"
DUMP_USER_PIP_REQ_FNAME = "requirements.txt"
SUPPORTED_PIP_REQ = [DUMP_USER_PIP_REQ_FNAME, "pip-req.txt", "pip3-req.txt"]

_DUMMY_FIELD = -1


class PythonVersionField(t.NamedTuple):
    major: int = _DUMMY_FIELD
    minor: int = _DUMMY_FIELD
    micro: int = _DUMMY_FIELD


def conda_install_req(
    req: t.Union[str, Path],
    env_name: str = "",
    prefix_path: t.Optional[Path] = None,
    enable_pre: bool = False,
) -> None:
    prefix_cmd = [get_conda_bin(), "run"]

    if env_name:
        prefix_cmd += ["--name", env_name]
    elif prefix_path is not None:
        prefix_cmd += ["--prefix", str(prefix_path.absolute())]

    prefix_cmd += ["python3", "-m", "pip"]
    _do_pip_install_req(prefix_cmd, req, enable_pre)


def _do_pip_install_req(
    prefix_cmd: t.List[t.Any], req: t.Union[str, Path], enable_pre: bool = False
) -> None:
    cmd = prefix_cmd + [
        "install",
        "--exists-action",
        "w",
    ]

    _env = os.environ
    if _env.get("SW_PYPI_INDEX_URL"):
        cmd += [
            "--index-url",
            _env["SW_PYPI_INDEX_URL"],
        ]
    if _env.get("SW_PYPI_EXTRA_INDEX_URL"):
        cmd += [
            "--extra-index-url",
            _env["SW_PYPI_EXTRA_INDEX_URL"],
        ]
    if _env.get("SW_PYPI_TRUSTED_HOST"):
        cmd += ["--trusted-host", _env["SW_PYPI_TRUSTED_HOST"]]

    if enable_pre:
        cmd += ["--pre"]

    if isinstance(req, Path):
        cmd += ["-r", str(req.absolute())]
    elif os.path.isfile(req):
        cmd += ["-r", req]
    else:
        cmd += [req]

    check_call(cmd)


def venv_install_req(
    venvdir: t.Union[str, Path], req: t.Union[str, Path], enable_pre: bool = False
) -> None:
    venvdir = str(venvdir)
    req = str(req)
    prefix_cmd = [os.path.join(venvdir, "bin", "pip")]
    _do_pip_install_req(prefix_cmd, req, enable_pre)


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
    python_version: str,
    prompt: str = "",
    clear: bool = False,
) -> None:
    # TODO: define starwhale virtualenv.py
    # TODO: use more elegant method to make venv portable
    args = [str(venvdir)]
    if prompt:
        args += ["--prompt", prompt]

    if python_version:
        _v = parse_python_version(python_version)
        args += ["--python", f"{_v.major}.{_v.minor}"]

    if clear:
        args += ["--clear"]

    session = virtualenv.cli_run(args)
    console.print(f":clap: create venv@{venvdir}, python:{session.interpreter.version}")  # type: ignore


def pip_freeze(
    py_env: str, path: t.Union[str, Path], include_editable: bool = False
) -> None:
    # TODO: add cmd timeout and error log
    _py_bin = get_user_runtime_python_bin(py_env)
    logger.info(f"{_py_bin}: pip freeze")
    cmd = [_py_bin, "-m", "pip", "freeze", "--require-virtualenv"]
    if not include_editable:
        cmd += ["--exclude-editable"]
    cmd += [">", str(path)]

    check_call(" ".join(cmd), shell=True)


def user_pip_install_pkg(py_env: str, pkg_name: str, enable_pre: bool = False) -> None:
    _py_bin = get_user_runtime_python_bin(py_env)
    cmd = [_py_bin, "-m", "pip", "install"]

    if enable_pre:
        cmd += ["--pre"]

    cmd += [pkg_name]
    check_call(cmd)


def check_python_interpreter_consistency(mode: str) -> t.Tuple[bool, str, str]:
    if mode == PythonRunEnv.CONDA:
        ep_base_prefix = os.environ.get(ENV_CONDA_PREFIX, "")
    elif mode == PythonRunEnv.VENV:
        ep_base_prefix = os.environ.get(ENV_VENV, "")
    else:
        ep_base_prefix = (
            os.environ.get(ENV_VENV)
            or os.environ.get(ENV_CONDA_PREFIX)
            or sys.base_prefix
        )
    logger.debug(
        f"current python interpreter base_prefix:{sys.base_prefix}, expected env base_prefix:{ep_base_prefix}"
    )
    _ok = ep_base_prefix == sys.base_prefix
    if not _ok:
        cur_version = f"{sys.version_info.major}.{sys.version_info.minor}"
        user_version = get_user_python_version(mode)
        if not user_version.startswith(cur_version):
            logger.error(
                f"swcli use python:{cur_version}, but runtime venv/conda python:{user_version}"
            )
            raise PythonEnvironmentError(
                f"swcli({cur_version}), runtime({user_version})"
            )

    return _ok, sys.base_prefix, ep_base_prefix


def guess_current_py_env() -> str:
    if is_venv():
        return PythonRunEnv.VENV
    elif is_conda():
        return PythonRunEnv.CONDA
    else:
        return PythonRunEnv.SYSTEM


def get_user_python_sys_paths(py_env: str) -> t.List[str]:
    logger.debug(f"get env({py_env}) sys path")
    _py_bin = get_user_runtime_python_bin(py_env)
    logger.info(f"{_py_bin}: sys.path")
    output = subprocess.check_output(
        [
            _py_bin,
            "-c",
            "import sys; print(','.join(sys.path))",
        ]
    )
    return output.decode().strip().split(",")


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


def conda_restore(
    env_fpath: t.Union[str, Path], target_env: t.Union[str, Path]
) -> None:
    cmd = f"{get_conda_bin()} env update --file {env_fpath} --prefix {target_env}"
    check_call(cmd, shell=True)


def conda_activate(env: t.Union[str, Path]) -> None:
    cmd = f"{get_conda_bin()} activate {env}"
    check_call(cmd, shell=True)


def conda_activate_render(env_dir: Path, workdir: Path) -> None:
    sw_cntr_content = """
_conda_hook="$(/opt/miniconda3/bin/conda shell.bash hook)"
cat >> /dev/stdout << EOF
$_conda_hook
conda activate /opt/starwhale/swmp/dep/conda/env
EOF
"""

    host_content = f"""
echo 'conda activate {env_dir.absolute()}'
"""
    _render_sw_activate(sw_cntr_content, host_content, workdir)


def venv_activate_render(
    venvdir: t.Union[str, Path], workdir: Path, relocate: bool = False
) -> None:
    bin = f"{venvdir}/bin"
    host_content = f"""
echo 'source {venvdir}/bin/activate'
"""

    if relocate:
        # TODO: support relocatable editable python package
        sw_cntr_content = f"""
sed -i '1d' {bin}/starwhale {bin}/sw {bin}/swcli {bin}/pip* {bin}/virtualenv
sed -i '1i\#!{bin}/python3' {bin}/starwhale {bin}/sw {bin}/swcli {bin}/pip* {bin}/virtualenv

sed -i 's#^VIRTUAL_ENV=.*$#VIRTUAL_ENV={venvdir}#g' {bin}/activate
rm -rf {bin}/python3
ln -s /usr/bin/python3 {bin}/python3
echo 'source {bin}/activate'
"""
    else:
        sw_cntr_content = host_content

    _render_sw_activate(sw_cntr_content, host_content, workdir)


def _render_sw_activate(sw_cntr_content: str, host_content: str, workdir: Path) -> None:
    _sw_path = workdir / "activate.sw"
    _host_path = workdir / "activate.host"

    ensure_file(_sw_path, sw_cntr_content, mode=0o755)
    ensure_file(_host_path, host_content, mode=0o755)

    console.print(
        f" :clap: {_sw_path.name} and {_host_path.name} is generated at {workdir}"
    )
    console.print(" :compass: run cmd:  ")
    console.print(f" \t Docker Container: [bold red] $(sh {_sw_path}) [/]")
    console.print(f" \t Host: [bold red] $(sh {_host_path}) [/]")


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
    identity: str = "",
) -> t.Dict[str, t.Any]:
    # TODO: smart dump python dep by starwhale sdk-api, pip ast analysis?
    dep_dir = Path(dep_dir)

    pr_env = get_python_run_env(mode)
    sys_name = platform.system()
    py_ver = get_user_python_version(pr_env)

    validate_python_environment(mode, expected_runtime, identity)
    validate_runtime_package_dep(mode)

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
        pip_freeze(pr_env, _pip_lock_req, include_editable)
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
            venv_setup(_venv_dir, python_version=expected_runtime)
            logger.info(
                f"[info:dep]install pip freeze({_pip_lock_req}) to venv: {_venv_dir}"
            )
            venv_install_req(_venv_dir, _pip_lock_req)
            if os.path.exists(pip_req_fpath):
                logger.info(
                    f"[info:dep]install custom pip({pip_req_fpath}) to venv: {_venv_dir}"
                )
                # TODO: support ignore editable package
                venv_install_req(_venv_dir, pip_req_fpath)
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


def get_user_python_version(py_env: str) -> str:
    _py_bin = get_user_runtime_python_bin(py_env)
    logger.info(f"{_py_bin}: python version")
    output = subprocess.check_output(
        [
            _py_bin,
            "-c",
            "import sys; _v=sys.version_info;print(f'{_v.major}.{_v.minor}.{_v.micro}')",
        ]
    )
    return output.decode().strip()


def get_user_runtime_python_bin(py_env: str) -> str:
    _prefix = get_base_prefix(py_env)
    _py_bin = os.path.join(_prefix, "bin", "python3")
    if not os.path.exists(_py_bin):
        raise NotFoundError(_py_bin)

    return _py_bin


def get_base_prefix(py_env: str) -> str:
    if py_env == PythonRunEnv.VENV:
        _path = os.environ.get(ENV_VENV, "")
    elif py_env == PythonRunEnv.CONDA:
        _path = os.environ.get(ENV_CONDA_PREFIX, "")
    else:
        _path = sys.prefix

    if _path and os.path.exists(_path):
        return _path
    else:
        raise NotFoundError(f"mode:{py_env}, base_prefix:{_path}")


def is_venv() -> bool:
    # TODO: refactor for get external venv attr
    output = subprocess.check_output(
        [
            "python3",
            "-c",
            "import sys; print(sys.prefix != (getattr(sys, 'base_prefix', None) or (getattr(sys, 'real_prefix', None) or sys.prefix)))",  # noqa: E501
        ],
    )
    return "True" in output.decode() or get_venv_env() != ""


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
            raise EnvironmentError("expected conda mode, but cannot find conda envs")
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
    workdir: Path,
    mode: str,
    python_version: str,
    local_gen_env: bool = False,
    pip_req: str = "",
) -> None:
    console.print(
        f":bread: restore python:{python_version} {mode}@{workdir}, use local env data: {local_gen_env}"
    )
    _f = _do_restore_conda if mode == PythonRunEnv.CONDA else _do_restore_venv
    _f(workdir, local_gen_env, python_version, pip_req)


def _do_restore_conda(
    _workdir: Path,
    _local_gen_env: bool,
    _python_version: str,
    _pip_req: str,
) -> None:
    _conda_dir = _workdir / "dep" / "conda"
    _tar_fpath = _conda_dir / CONDA_ENV_TAR
    _env_dir = _conda_dir / "env"
    _python_dir = _workdir / "dep" / "python"

    if _local_gen_env and _tar_fpath.exists():
        empty_dir(_env_dir)
        ensure_dir(_env_dir)
        logger.info(f"extract {_tar_fpath} ...")
        with tarfile.open(str(_tar_fpath)) as f:
            f.extractall(str(_env_dir))
        venv_activate_render(_env_dir, _workdir)
    else:
        logger.info("restore conda env ...")
        _env_yaml = _conda_dir / DUMP_CONDA_ENV_FNAME
        conda_restore(_env_yaml, _env_dir)
        _pip_req_path = _python_dir / _pip_req
        if _pip_req_path.exists():
            conda_install_req(req=_pip_req_path, prefix_path=_env_dir)
        conda_activate_render(_env_dir, _workdir)


def _do_restore_venv(
    _workdir: Path,
    _local_gen_env: bool,
    _python_version: str,
    pip_req: str,
    _rebuild: bool = False,
) -> None:
    _python_dir = _workdir / "dep" / "python"
    _venv_dir = _python_dir / "venv"

    _relocate = True
    if _rebuild or not _local_gen_env or not (_venv_dir / "bin" / "activate").exists():
        logger.info(f"setup venv and pip install {_venv_dir}")
        _relocate = False
        venv_setup(_venv_dir, python_version=_python_version)
        for _name in (DUMP_PIP_REQ_FNAME, pip_req):
            _path = _python_dir / _name
            if not _name or not _path.exists():
                continue

            logger.info(f"pip install {_path} ...")
            venv_install_req(_venv_dir, _path)

    venv_activate_render(_venv_dir, _workdir, relocate=_relocate)


def validate_runtime_package_dep(py_env: str) -> None:
    _py_bin = get_user_runtime_python_bin(py_env)
    logger.info(f"{_py_bin}: check {SW_PYPI_PKG_NAME} install")
    cmd = [
        _py_bin,
        "-c",
        f"import pkg_resources; pkg_resources.get_distribution('{SW_PYPI_PKG_NAME}')",
    ]
    try:
        check_call(cmd)
    except subprocess.CalledProcessError:
        console.print(
            f":confused_face: Please install {SW_PYPI_PKG_NAME} in {py_env}, cmd:"
        )
        console.print(
            f"\t :cookie: python3 -m pip install --pre {SW_PYPI_PKG_NAME} :cookie:"
        )
        raise


def validate_python_environment(mode: str, py_version: str, identity: str = "") -> None:
    # TODO: add os platform check
    current_py_env = get_python_run_env(mode)
    current_py_version = get_user_python_version(current_py_env)

    if py_version and not current_py_version.startswith(py_version):
        raise EnvironmentError(
            f"expected python({py_version}) is not equal to detected python({current_py_version})"
        )

    if current_py_env != mode:
        raise EnvironmentError(
            f"expected mode({mode}), detected mode({current_py_env})"
        )

    # TODO: add venv identity check
    if mode == PythonRunEnv.CONDA and not identity:
        conda_name = os.environ.get(ENV_CONDA, "")
        if conda_name != identity:
            raise EnvironmentError(
                f"expected conda name({identity}), detected current conda name({conda_name})"
            )
