import os
import sys
import shutil
import typing as t
import platform
import subprocess
from pathlib import Path, PurePath, PosixPath
from functools import lru_cache

import yaml
import conda_pack
import virtualenv

from starwhale.utils import (
    console,
    is_linux,
    load_yaml,
    venv_pack,
    get_downloadable_sw_version,
)
from starwhale.consts import (
    ENV_VENV,
    ENV_CONDA,
    PythonRunEnv,
    ENV_LOG_LEVEL,
    ENV_CONDA_PREFIX,
    SW_PYPI_PKG_NAME,
    SW_DEV_DUMMY_VERSION,
    WHEEL_FILE_EXTENSION,
    DEFAULT_CONDA_CHANNEL,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.utils.fs import ensure_dir, ensure_file, extract_tar
from starwhale.utils.error import (
    FormatError,
    ExistedError,
    NotFoundError,
    NoSupportError,
    ParameterError,
    MissingFieldError,
    PythonEnvironmentError,
)
from starwhale.utils.process import check_call

SUPPORTED_PIP_REQ = ["requirements.txt", "pip-req.txt", "pip3-req.txt"]

_DUMMY_FIELD = -1

_ConfigsT = t.Optional[t.Dict[str, t.Dict[str, t.Union[str, t.List[str]]]]]
_PipConfigT = t.Optional[t.Dict[str, t.Union[str, t.List[str]]]]
_PipReqT = t.Union[str, Path, PosixPath]


class EnvTarType:
    CONDA = "conda_env.tar.gz"
    VENV = "venv_env.tar.gz"


class PythonVersionField(t.NamedTuple):
    major: int = _DUMMY_FIELD
    minor: int = _DUMMY_FIELD
    micro: int = _DUMMY_FIELD


def conda_install_req(
    req: t.Union[str, Path, t.List],
    env_name: str = "",
    prefix_path: t.Optional[Path] = None,
    enable_pre: bool = False,
    use_pip_install: bool = True,
    configs: _ConfigsT = None,
) -> None:
    if not req:
        return

    if not isinstance(req, list):
        req = [req]

    configs = configs or {}
    prefix_cmd = [get_conda_bin()]

    if use_pip_install:
        prefix_cmd += ["run", "--live-stream"]
    else:
        prefix_cmd += ["install"]

    verbose = get_conda_log_verbose()
    if verbose:
        prefix_cmd += [verbose]

    if env_name:
        prefix_cmd += ["--name", env_name]
    elif prefix_path is not None:
        prefix_cmd += ["--prefix", str(prefix_path.absolute())]

    if use_pip_install:
        prefix_cmd += ["python3", "-m", "pip"]
        _do_pip_install_req(prefix_cmd, req, enable_pre, configs.get("pip"))  # type: ignore
    else:
        channels = configs.get("conda", {}).get("channels", [DEFAULT_CONDA_CHANNEL])
        for _c in channels:
            prefix_cmd += ["--channel", _c]

        prefix_cmd += ["--yes", "--override-channels"]
        check_call(prefix_cmd + [str(r) for r in req])


def _do_pip_install_req(
    prefix_cmd: t.List[t.Any],
    req: t.Union[str, Path, t.List],
    enable_pre: bool = False,
    pip_config: _PipConfigT = None,
) -> None:
    cmd = prefix_cmd + [
        "install",
        "--exists-action",
        "w",
    ]

    pip_config = pip_config or {}
    _env = os.environ

    list_to_str: t.Callable[[t.Union[str, list]], str] = (
        lambda x: " ".join(x) if isinstance(x, list) else x
    )

    # TODO: remove SW_PYPI_* envs

    _index_url = _env.get("SW_PYPI_INDEX_URL", "")
    _extra_index_urls = [
        _env.get("SW_PYPI_EXTRA_INDEX_URL", ""),
        list_to_str(pip_config.get("extra_index_url", [])),
    ]
    _hosts = [
        _env.get("SW_PYPI_TRUSTED_HOST", ""),
        list_to_str(pip_config.get("trusted_host", [])),
    ]

    config_index_url = list_to_str(pip_config.get("index_url", ""))
    if _index_url:
        _extra_index_urls.append(config_index_url)
    else:
        _index_url = config_index_url

    _s_index = _index_url.strip()
    _s_extra_index = " ".join([s for s in _extra_index_urls if s.strip()])
    _s_hosts = " ".join([s for s in _hosts if s.strip()])

    if _s_index:
        cmd += ["--index-url", _s_index]
    if _s_extra_index:
        cmd += ["--extra-index-url", _s_extra_index]
    if _s_hosts:
        cmd += ["--trusted-host", _s_hosts]

    cmd += [
        f"--timeout={_env.get('SW_PYPI_TIMEOUT', 90)}",
        f"--retries={_env.get('SW_PYPI_RETRIES', 10)}",
    ]

    if enable_pre:
        cmd += ["--pre"]

    if not isinstance(req, list):
        req = [req]

    for r in req:
        if isinstance(r, PurePath):
            if not r.name.endswith(WHEEL_FILE_EXTENSION):
                cmd += ["-r"]
            cmd += [str(r.absolute())]  # type: ignore
        elif os.path.isfile(r):
            if not r.endswith(WHEEL_FILE_EXTENSION):
                cmd += ["-r"]
            cmd += [r]
        else:
            cmd += [r]

    check_call(cmd)


def venv_install_req(
    venvdir: t.Union[str, Path],
    req: _PipReqT,
    enable_pre: bool = False,
    pip_config: _PipConfigT = None,
) -> None:
    if not req:
        return

    venvdir = str(venvdir)
    req = str(req)
    prefix_cmd = [os.path.join(venvdir, "bin", "pip")]
    _do_pip_install_req(prefix_cmd, req, enable_pre, pip_config)


def venv_activate(venvdir: t.Union[str, Path]) -> None:
    _fpath = Path(venvdir) / "bin" / "activate"
    cmd = f"source {_fpath.absolute()}"
    check_call(cmd, shell=True, executable="/bin/bash")


def render_python_env_activate(
    mode: str,
    prefix_path: Path,
    workdir: Path,
    local_packaged_env: bool = False,
    verbose: bool = True,
) -> None:
    if mode not in (PythonRunEnv.CONDA, PythonRunEnv.VENV):
        raise NoSupportError(f"mode({mode}) render python env activate scripts")

    if local_packaged_env:
        # conda local mode(conda-pack) should be activated by the source command.
        venv_activate_render(prefix_path, workdir, relocate=mode == PythonRunEnv.VENV)
    else:
        if mode == PythonRunEnv.CONDA:
            conda_activate_render(prefix_path, workdir, verbose=verbose)
        else:
            venv_activate_render(prefix_path, workdir, relocate=False, verbose=verbose)


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


def get_user_pip_config_by_pybin(py_bin: str) -> t.Dict[str, str]:
    output = subprocess.check_output([py_bin, "-m", "pip", "config", "list", "--user"])
    config: t.Dict[str, str] = {}
    for line in output.decode().strip().split("\n"):
        line = line.strip()
        if not line or "=" not in line:
            continue

        k, v = line.split("=", 1)
        config[k] = v
    return config


def pip_freeze_by_pybin(
    py_bin: str,
    lock_fpath: t.Union[str, Path],
    include_editable: bool = False,
    emit_options: bool = False,
    include_local_wheel: bool = False,
) -> None:
    lock_fpath = Path(lock_fpath)
    console.info(f"{py_bin}: pip freeze...")

    content = [f"# Generated by Starwhale({STARWHALE_VERSION}) Runtime Lock"]

    if not emit_options:
        _pip_config = get_user_pip_config_by_pybin(py_bin)
        if _pip_config.get("global.index-url"):
            content.append(f"--index-url {_pip_config['global.index-url']}")

        if _pip_config.get("global.extra-index-url"):
            content.append(f"--extra-index-url {_pip_config['global.extra-index-url']}")

        if _pip_config.get("install.trusted-host"):
            content.append(f"--trusted-host {_pip_config['install.trusted-host']}")

    ensure_file(lock_fpath, "\n".join(content) + "\n")
    cmd = [py_bin, "-m", "pip", "freeze", "--require-virtualenv"]
    if not include_editable:
        cmd += ["--exclude-editable"]
    cmd += [">>", str(lock_fpath)]

    check_call(" ".join(cmd), shell=True)

    if not include_local_wheel:
        content = []
        for line in lock_fpath.read_text().splitlines():
            # local wheel case:
            # example @ file:///path/to/example-0.0.0-cp39-cp39-linux_x86_64.whl
            # example @ file:///path/to/example-0.0.0-cp39-cp39-linux_x86_64.whl#sha256=23627
            if "@ file://" in line and (line.endswith(".whl") or ".whl#" in line):
                continue
            content.append(line)
        ensure_file(lock_fpath, "\n".join(content))


def pip_freeze(
    py_env: str,
    lock_fpath: t.Union[str, Path],
    include_editable: bool = False,
    emit_options: bool = False,
    include_local_wheel: bool = False,
) -> None:
    # TODO: add cmd timeout and error log
    _py_bin = get_user_runtime_python_bin(py_env)
    pip_freeze_by_pybin(
        py_bin=_py_bin,
        lock_fpath=lock_fpath,
        include_editable=include_editable,
        emit_options=emit_options,
        include_local_wheel=include_local_wheel,
    )


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
    console.debug(
        f"current python interpreter base_prefix:{sys.base_prefix}, expected env base_prefix:{ep_base_prefix}"
    )
    _ok = ep_base_prefix == sys.base_prefix
    if not _ok:
        cur_version = get_python_version()
        user_version = get_user_python_version(mode)
        if not user_version.startswith(cur_version):
            console.error(
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
    console.debug(f"get env({py_env}) sys path")
    _py_bin = get_user_runtime_python_bin(py_env)
    console.info(f"{_py_bin}: sys.path")
    output = subprocess.check_output(
        [
            _py_bin,
            "-c",
            "import sys; print(','.join(sys.path))",
        ]
    )
    return output.decode().strip().split(",")


def conda_setup(
    python_version: str,
    name: str = "",
    prefix: t.Union[str, Path] = "",
) -> None:
    if not name and not prefix:
        raise ParameterError("conda setup must set name or prefix")

    cmd = [get_conda_bin(), "create", "--yes", "--quiet"]

    if name:
        cmd += ["--name", name]

    if prefix:
        cmd += ["--prefix", str(prefix)]

    verbose = get_conda_log_verbose()
    if verbose:
        cmd += [verbose]

    cmd += [f"python={trunc_python_version(python_version)}"]
    check_call(cmd)


def conda_export(
    lock_fpath: t.Union[str, Path],
    prefix: str,
    include_local_wheel: bool = False,
    include_editable: bool = False,
) -> None:
    # TODO: add cmd timeout
    conda_export_cmd = [
        get_conda_bin(),
        "env",
        "export",
        "--prefix",
        prefix,
        "--file",
        str(lock_fpath),
    ]
    check_call(conda_export_cmd)

    pip_freeze_cmd = [
        get_conda_bin(),
        "run",
        "--prefix",
        prefix,
        "pip",
        "freeze",
    ]
    if not include_editable:
        pip_freeze_cmd += ["--exclude-editable"]

    pip_freeze_list = []
    output = subprocess.check_output(pip_freeze_cmd)
    for line in output.decode().strip().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue

        if not include_local_wheel and (line.endswith(".whl") or "@ file://" in line):
            continue

        pip_freeze_list.append(line)

    # conda cannot export URL for pip packages installed from git: https://github.com/conda/conda/issues/10320
    # we use pip freeze to fix this issue.
    conda_yaml = load_yaml(lock_fpath)
    dependencies = []
    for d in conda_yaml.get("dependencies", []):
        if isinstance(d, dict) and "pip" in d:
            if pip_freeze_list:
                d["pip"] = pip_freeze_list
            else:
                d.pop("pip", None)

        if d:
            dependencies.append(d)
    conda_yaml["dependencies"] = dependencies
    ensure_file(lock_fpath, yaml.safe_dump(conda_yaml, default_flow_style=False))


def conda_env_update(
    env_fpath: t.Union[str, Path], target_env: t.Union[str, Path]
) -> None:
    target_env = Path(target_env).resolve()
    cmd = [get_conda_bin(), "env", "update"]
    verbose = get_conda_log_verbose()
    if verbose:
        cmd += [verbose]

    cmd += [
        "--quiet",
        "--file",
        str(env_fpath),
        "--prefix",
        str(target_env),
    ]
    check_call(cmd)


def conda_activate_render(env_dir: Path, workdir: Path, verbose: bool = True) -> None:
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
    _render_sw_activate(sw_cntr_content, host_content, workdir, verbose)


def venv_activate_render(
    venvdir: t.Union[str, Path],
    workdir: Path,
    relocate: bool = False,
    verbose: bool = True,
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

    _render_sw_activate(sw_cntr_content, host_content, workdir, verbose)


def _render_sw_activate(
    sw_cntr_content: str, host_content: str, workdir: Path, verbose: bool = True
) -> None:
    _sw_path = workdir / "activate.sw"
    _host_path = workdir / "activate.host"

    ensure_file(_sw_path, sw_cntr_content, mode=0o755)
    ensure_file(_host_path, host_content, mode=0o755)

    if verbose:
        console.print(
            f":clap: {_sw_path.name} and {_host_path.name} is generated at {workdir}"
        )


def get_conda_prefix_path(name: str = "") -> str:
    cmd = [get_conda_bin(), "run"]
    if name:
        cmd += ["--name", name]

    cmd += ["printenv", "CONDA_PREFIX"]
    output = subprocess.check_output(cmd)
    return output.decode().strip()


def get_conda_log_verbose(lvl_name: str = "") -> str:
    lvl_name = lvl_name or os.environ.get(ENV_LOG_LEVEL, "")
    lvl_name = lvl_name.upper()
    if lvl_name == "TRACE":
        return "-vvv"
    elif lvl_name == "DEBUG":
        return "-vv"
    elif lvl_name == "INFO":
        return "-v"
    else:
        return ""


@lru_cache()
def get_conda_bin() -> str:
    for _p in (
        os.environ.get("CONDA_EXE", ""),
        "/opt/miniconda3/bin/conda",
        "/opt/anaconda3/bin/conda",
        "/usr/local/miniconda3/bin/conda",
        "/usr/local/anaconda3/bin/conda",
        os.path.expanduser("~/miniconda3/bin/conda"),
        os.path.expanduser("~/anaconda3/bin/conda"),
    ):
        if _p and os.path.exists(_p):
            return _p
    else:
        return "conda"


def package_python_env(
    export_dir: t.Union[str, Path],
    mode: str,
    env_prefix_path: str = "",
    env_name: str = "",
    include_editable: bool = False,
) -> bool:
    export_dir = Path(export_dir)

    sys_name = platform.system()
    if not is_linux():
        console.warning(
            f"[info:dep]{sys_name} will skip conda/venv to generate local all bundles env"
        )
        return False

    ensure_dir(export_dir)
    dest_tar_path = export_dir / (
        EnvTarType.CONDA if mode == PythonRunEnv.CONDA else EnvTarType.VENV
    )

    if mode == PythonRunEnv.CONDA:
        env_name = env_name or get_conda_env()
        if not env_name and not env_prefix_path:
            raise Exception(
                "conda package must use env_name/env_prefix_path or be in the conda activated environment"
            )

        console.print(
            f":package: try to package conda env_name:{env_name}, env_prefix_path:{env_prefix_path}..."
        )

        conda_pack.pack(
            name=env_name,
            prefix=env_prefix_path,
            force=True,
            output=str(dest_tar_path),
            ignore_editable_packages=not include_editable,
        )
    elif mode == PythonRunEnv.VENV:
        env_prefix_path = env_prefix_path or get_venv_env()

        if include_editable:
            raise NoSupportError("venv exports editable packages")

        if not env_prefix_path:
            raise Exception(
                "virtualenv package must use env_prefix_path or be in the virtualenv activated environment"
            )

        console.print(f":package: try to package virtualenv {env_prefix_path}...")
        venv_pack.pack(
            prefix=env_prefix_path,
            force=True,
            output=str(dest_tar_path),
        )
    else:
        raise NoSupportError(f"package python env in {mode} mode")

    console.print(f":beer_mug: pack @ [underline]{dest_tar_path}[/]")
    return True


def activate_python_env(mode: str, identity: str, interactive: bool) -> None:
    if mode == PythonRunEnv.VENV:
        cmd = f"source {identity}/bin/activate"
    elif mode == PythonRunEnv.CONDA:
        cmd = f"conda activate {identity}"
    else:
        raise NoSupportError(mode)

    if interactive:
        import shellingham

        try:
            _name, _bin = shellingham.detect_shell()
        except shellingham.ShellDetectionFailure:
            _name, _bin = "", ""

        if not _bin.startswith("/") or _name == _bin:
            _bin = shutil.which(_name) or _bin

        if _name == "zsh":
            # https://zsh.sourceforge.io/Intro/intro_3.html
            os.execl(
                _bin,
                _bin,
                "-c",
                f"""temp_dir={identity} && \
                echo ". $HOME/.zshrc && {cmd}" > $temp_dir/.zshrc && \
                ZDOTDIR=$temp_dir zsh -i""",
            )
        elif _name == "bash":
            # https://www.gnu.org/software/bash/manual/html_node/Bash-Startup-Files.html
            os.execl(
                _bin, _bin, "-c", f'bash --rcfile <(echo ". "$HOME/.bashrc" && {cmd}")'
            )
        elif _name == "fish":
            # https://fishshell.com/docs/current/language.html#configuration
            os.execl(_bin, _bin, "-C", cmd)

    # user executes the command manually
    console.print(":cake: run command in shell :cake:")
    console.print(f"\t[red][bold]{cmd}")


def create_python_env(
    mode: str,
    name: str,
    isolated_env_dir: Path,
    python_version: str,
    force: bool = False,
) -> None:
    if mode == PythonRunEnv.VENV:
        if isolated_env_dir.exists() and not force:
            raise ExistedError(str(isolated_env_dir))

        console.info(f"create venv @ {isolated_env_dir}...")
        venv_setup(isolated_env_dir, python_version=python_version, prompt=name)
    elif mode == PythonRunEnv.CONDA:
        console.info(
            f"create conda {name}:{isolated_env_dir}, use python {python_version}..."
        )
        conda_setup(python_version, prefix=isolated_env_dir)
    else:
        raise NoSupportError(mode)


def get_python_version() -> str:
    return f"{sys.version_info.major}.{sys.version_info.minor}"


def pip_compatible_dependencies_check(py_bin: t.Optional[str] = None) -> None:
    py_bin = py_bin or sys.executable
    check_call([py_bin, "-m", "pip", "check"])


def get_python_version_by_bin(py_bin: str) -> str:
    console.info(f"{py_bin}: python version")
    output = subprocess.check_output(
        [
            py_bin,
            "-c",
            "import sys; _v=sys.version_info;print(f'{_v.major}.{_v.minor}.{_v.micro}')",
        ]
    )
    return output.decode().strip()


def get_conda_pybin(prefix: t.Union[str, Path] = "", name: str = "") -> str:
    if prefix:
        return str(Path(prefix) / "bin" / "python3")

    if name:
        output = subprocess.check_output(
            [get_conda_bin(), "run", "-n", name, "which", "python3"]
        )
        return output.decode().strip()

    raise MissingFieldError("set prefix or name parameter")


def get_user_python_version(py_env: str) -> str:
    _py_bin = get_user_runtime_python_bin(py_env)
    return get_python_version_by_bin(_py_bin)


def get_user_runtime_python_bin(py_env: str) -> str:
    _prefix = get_base_prefix(py_env)
    _py_bin = os.path.join(_prefix, "bin", "python3")
    if not os.path.exists(_py_bin):
        raise NotFoundError(_py_bin)

    return _py_bin


def check_valid_venv_prefix(prefix: t.Union[str, Path]) -> bool:
    expect = Path(prefix) / "pyvenv.cfg"
    return expect.exists() and expect.is_file()


def check_valid_conda_prefix(prefix: t.Union[str, Path]) -> bool:
    expect = Path(prefix) / "conda-meta"
    return expect.exists() and expect.is_dir()


def guess_python_env_mode(prefix: t.Union[str, Path]) -> str:
    if check_valid_venv_prefix(prefix):
        return PythonRunEnv.VENV
    elif check_valid_conda_prefix(prefix):
        return PythonRunEnv.CONDA
    else:
        raise NoSupportError(f"guess python env from paht: {prefix}")


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
            "import sys; print(sys.prefix != (getattr(sys, 'base_prefix', None) or (getattr(sys, 'real_prefix', None) or sys.prefix)))",
            # noqa: E501
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


def install_starwhale(
    prefix_path: Path,
    mode: str,
    version: str = "",
    force: bool = False,
    configs: _ConfigsT = None,
) -> None:
    if version == "" or version == SW_DEV_DUMMY_VERSION:
        version = get_downloadable_sw_version()

    req = SW_PYPI_PKG_NAME
    if version:
        req = f"{req}=={version}"

    _existed = check_user_python_pkg_exists(
        str(prefix_path / "bin" / "python3"), SW_PYPI_PKG_NAME
    )
    if _existed and not force:
        console.info(f"{SW_PYPI_PKG_NAME} has already be installed at {prefix_path}")
        return

    configs = configs or {}
    if mode == PythonRunEnv.CONDA:
        conda_install_req(
            req=req, prefix_path=prefix_path, use_pip_install=True, configs=configs
        )
    elif mode == PythonRunEnv.VENV:
        venv_install_req(venvdir=prefix_path, req=req, pip_config=configs.get("pip"))
    else:
        raise NoSupportError(f"mode({mode}) install {SW_PYPI_PKG_NAME}")


def extract_conda_pkg(workdir: Path, isolated_env_dir: t.Optional[Path] = None) -> None:
    export_dir = workdir / "export"
    export_tar_fpath = export_dir / EnvTarType.CONDA
    conda_dir = isolated_env_dir or export_dir / "conda"
    extract_tar(export_tar_fpath, conda_dir, force=True)
    # TODO: conda local bundle restore wheel?


def extract_venv_pkg(workdir: Path, isolated_env_dir: t.Optional[Path] = None) -> None:
    export_dir = workdir / "export"
    venv_dir = isolated_env_dir or export_dir / "venv"
    export_tar_fpath = export_dir / EnvTarType.VENV
    extract_tar(export_tar_fpath, venv_dir, force=True)


def check_user_python_pkg_exists(py_bin: str, pkg_name: str) -> bool:
    cmd = [
        py_bin,
        "-c",
        f"import pkg_resources; pkg_resources.get_distribution('{pkg_name}')",
    ]
    try:
        check_call(cmd)
    except subprocess.CalledProcessError:
        return False
    else:
        return True


def trunc_python_version(python_version: str) -> str:
    _tp = python_version.strip().split(".")
    # TODO: support python full version format: {major}:{minor}:{micro}
    return ".".join(_tp[:2])
