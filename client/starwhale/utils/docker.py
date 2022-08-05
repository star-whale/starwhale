import os
import typing as t
import subprocess
from pathlib import Path

from starwhale.utils import console
from starwhale.consts import SupportArch
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
