import os
import sys
import typing as t
import getpass as gt
import subprocess
from pwd import getpwnam
from pathlib import Path

from starwhale.utils import config, console
from starwhale.consts import SupportArch, CNTR_DEFAULT_PIP_CACHE_DIR
from starwhale.utils.error import NoSupportError, MissingFieldError
from starwhale.utils.process import check_call

BUILDX_ARCH_MAP = {SupportArch.AMD64: "linux/amd64", SupportArch.ARM64: "linux/arm64"}
BUILDER_NAME = "starwhale-multiarch-runtime-builder"

# make Docker-ce 19.03 happy to use buildx
_BUILDX_CMD_ENV = {"DOCKER_CLI_EXPERIMENTAL": "enabled"}


def reset_qemu_static() -> None:
    # ref: https://github.com/multiarch/qemu-user-static
    check_call(
        [
            "docker",
            "run",
            "--rm",
            "--privileged",
            "multiarch/qemu-user-static",
            "--reset",
            "-p",
            "-yes",
        ],
        log=console.print,
    )


def check_builder_exists() -> bool:
    cmd = [
        "docker",
        "buildx",
        "inspect",
        "--builder",
        BUILDER_NAME,
    ]
    try:
        check_call(cmd, env=_BUILDX_CMD_ENV)
    except subprocess.CalledProcessError as e:
        if "no builder" in e.output:
            return False
        raise

    return True


def create_builder(dry_run: bool) -> None:
    # ref: https://docs.docker.com/build/buildx/drivers/ for multi-arch build
    if not dry_run and check_builder_exists():
        console.print(f"{BUILDER_NAME} already exists, skip create")
        return

    cmd = [
        "docker",
        "buildx",
        "create",
        "--bootstrap",
        "--name",
        BUILDER_NAME,
        "--driver",
        "docker-container",
    ]

    _http_proxy = os.environ.get("HTTP_PROXY") or os.environ.get("http_proxy")
    _https_proxy = os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy")
    if _http_proxy:
        cmd += [
            "--driver-opt",
            f"env.http_proxy={_http_proxy}",
            "--driver-opt",
            f"env.HTTP_PROXY={_http_proxy}",
        ]

    if _https_proxy:
        cmd += [
            "--driver-opt",
            f"env.https_proxy={_https_proxy}",
            "--driver-opt",
            f"env.HTTPS_PROXY={_https_proxy}",
        ]

    cmd += ["--driver-opt", "network=host"]

    if dry_run:
        console.print(":tiger_face: buildx create command:")
        console.print(f"\t [bold red]{' '.join(cmd)}[/]")
    else:
        console.print(":tiger_face: start to create buildx instance...")
        check_call(cmd, log=console.print, env=_BUILDX_CMD_ENV)


def buildx(
    dockerfile_path: Path,
    workdir: Path,
    tags: t.List[str],
    platforms: t.List[str],
    push: bool = False,
    dry_run: bool = False,
    use_starwhale_builder: bool = False,
) -> None:
    if not tags:
        raise MissingFieldError(
            "buildx must contain at least one or more tag parameter"
        )

    cmd = [
        "docker",
        "buildx",
        "build",
        "--network",
        "host",
        "--pull",
        "--progress",
        "plain",
    ]

    if use_starwhale_builder:
        create_builder(dry_run)
        cmd += ["--builder", BUILDER_NAME]

    for _p in platforms:
        # TODO: add multiarch/qemu-user-static reset for non-amd64 arch
        if _p not in BUILDX_ARCH_MAP:
            raise NoSupportError(f"Starwhale base image {_p} arch")
        cmd += ["--platform", BUILDX_ARCH_MAP[_p]]

    for _t in tags:
        _t = _t.strip()
        if not _t:
            continue
        cmd += ["--tag", _t]

    if push:
        cmd += ["--push"]
    else:
        cmd += ["--load"]

    cmd += ["--file", str(dockerfile_path.absolute()), str(workdir.absolute())]

    if dry_run:
        console.print(":panda_face: buildx build command:")
        console.print(f"\t [bold red]{' '.join(cmd)}[/]")
    else:
        console.print(":panda_face: start to build image with buildx...")
        check_call(cmd, log=console.print, env=_BUILDX_CMD_ENV)


def gen_swcli_docker_cmd(
    image: str,
    envs: t.Optional[t.Dict[str, str]] = None,
    mounts: t.Optional[t.List[str]] = None,
    name: str = "",
) -> str:
    if not image:
        raise ValueError("image should have value")
    pwd = os.getcwd()

    rootdir = config.load_swcli_config()["storage"]["root"]
    config_path = config.get_swcli_config_path()
    cmd = [
        "docker",
        "run",
        "--net=host",
        "--rm",
        "-e",
        "DEBUG=1",
        "-e",
        f"SW_USER={gt.getuser()}",
        "-e",
        f"SW_USER_ID={getpwnam(gt.getuser()).pw_uid}",
        "-e",
        "SW_USER_GROUP_ID=0",
        "-e",
        f"SW_LOCAL_STORAGE={rootdir}",
        "-v",
        f"{rootdir}:{rootdir}",
        "-e",
        f"SW_CLI_CONFIG={config_path}",
        "-v",
        f"{config_path}:{config_path}",
        "-v",
        f"{pwd}:{pwd}",
        "-w",
        f"{pwd}",
    ]

    if name:
        cmd += [
            "--name",
            f'"{name}"',
        ]

    mounts = mounts or []
    for mount in mounts:
        cmd += [
            "-v",
            f"{mount}:{mount}",
        ]

    envs = envs or {}
    for _k, _v in envs.items():
        cmd.extend(["-e", f"{_k}={_v}"])

    cntr_cache_dir = os.environ.get("SW_PIP_CACHE_DIR", CNTR_DEFAULT_PIP_CACHE_DIR)
    host_cache_dir = os.path.expanduser("~/.cache/starwhale-pip")
    cmd += ["-v", f"{host_cache_dir}:{cntr_cache_dir}"]

    _env = os.environ
    for _ee in (
        "SW_PYPI_INDEX_URL",
        "SW_PYPI_EXTRA_INDEX_URL",
        "SW_PYPI_TRUSTED_HOST",
        "SW_PYPI_TIMEOUT",
        "SW_PYPI_RETRIES",
    ):
        if _ee not in _env:
            continue
        cmd.extend(["-e", f"{_ee}={_env[_ee]}"])

    sw_cmd = " ".join([item for item in sys.argv[1:] if "in-container" not in item])

    cmd.extend([image, f"swcli {sw_cmd}"])
    return " ".join(cmd)
