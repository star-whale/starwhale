import os
import typing as t
import subprocess
from copy import deepcopy

import click
from jinja2 import Environment, FileSystemLoader

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
jinja2_env = Environment(loader=FileSystemLoader(searchpath=CURRENT_DIR))

GHCR_IO_IMAGE_NAME = "ghcr.io/star-whale/cuda"
DOCKER_IO_IMAGE_NAME = "docker.io/starwhaleai/cuda"


class _CUDNN(t.NamedTuple):
    full_version: str
    support_cuda_versions: t.List[str]


class _CUDA(t.NamedTuple):
    require_amd64: str
    cuart_version: str
    full_version: str
    nvtx_version: str
    libnpp_version: str
    libcusparse_version: str
    libcublas_version: str
    nccl_version: str
    cudnn_version: str = ""
    require_arm64: str = ""


_CUDNN_META = {
    "8.4": _CUDNN(
        full_version="8.4.1.50",
        support_cuda_versions=["11.3", "11.4", "11.5", "11.6"],
    )
}

_CUDA_META = {
    "11.3": _CUDA(
        require_amd64="brand=tesla,driver>=418,driver<419 driver>=450",
        cuart_version="11.3.109-1",
        nvtx_version="11.3.109-1",
        libnpp_version="11.3.3.95-1",
        libcusparse_version="11.6.0.109-1",
        libcublas_version="11.5.1.109-1",
        nccl_version="2.9.9-1",
        full_version="11.3.1",
        cudnn_version="8.2.1.32",
    ),
    "11.4": _CUDA(
        require_amd64="brand=tesla,driver>=418,driver<419 brand=tesla,driver>=450,driver<451",
        cuart_version="11.4.148-1",
        nvtx_version="11.4.120-1",
        libnpp_version="11.4.0.110-1",
        libcusparse_version="11.6.0.120-1",
        libcublas_version="11.6.5.2-1",
        nccl_version="2.11.4-1",
        full_version="11.4.3",
        cudnn_version="8.2.2.26",
    ),
    "11.5": _CUDA(
        require_amd64="brand=tesla,driver>=418,driver<419 brand=tesla,driver>=450,driver<451 brand=tesla,driver>=470,driver<471",
        cuart_version="11.5.117-1",
        nvtx_version="11.5.114-1",
        libnpp_version="11.5.1.107-1",
        libcusparse_version="11.7.0.107-1",
        libcublas_version="11.7.4.6-1",
        nccl_version="2.11.4-1",
        full_version="11.5.2",
        cudnn_version="8.3.3.40",
    ),
    "11.6": _CUDA(
        require_amd64="brand=tesla,driver>=418,driver<419 brand=tesla,driver>=450,driver<451 brand=tesla,driver>=470,driver<471 brand=unknown,driver>=470,driver<471 brand=nvidia,driver>=470,driver<471 brand=nvidiartx,driver>=470,driver<471 brand=quadrortx,driver>=470,driver<471",
        cuart_version="11.6.55-1",
        nvtx_version="11.6.124-1",
        libnpp_version="11.6.3.124-1",
        libcusparse_version="11.7.2.124-1",
        libcublas_version="11.9.2.110-1",
        nccl_version="2.12.10-1",
        full_version="11.6.2",
        cudnn_version="8.4.1.50",
    ),
    "11.7": _CUDA(
        require_amd64="brand=tesla,driver>=450,driver<451 brand=tesla,driver>=470,driver<471 brand=unknown,driver>=470,driver<471 brand=nvidia,driver>=470,driver<471 brand=nvidiartx,driver>=470,driver<471 brand=quadrortx,driver>=470,driver<471 brand=unknown,driver>=510,driver<511 brand=nvidia,driver>=510,driver<511 brand=nvidiartx,driver>=510,driver<511 brand=quadrortx,driver>=510,driver<511",
        cuart_version="11.7.60-1",
        nvtx_version="11.7.50-1",
        libnpp_version="11.7.3.21-1",
        libcusparse_version="11.7.3.50-1",
        libcublas_version="11.10.1.25-1",
        nccl_version="2.13.4-1",
        full_version="11.7.0",
        cudnn_version="",  # cuda 11.7 no support cudnn
    ),
}


@click.command()
@click.option(
    "--all", is_flag=True, help="Generate dockerfile from all of the cuda/cudnn version"
)
@click.option(
    "--cuda",
    required=False,
    type=click.Choice(_CUDA_META.keys()),
    help="CUDA Version",
)
@click.option("-d", "--dest-dir", default="", help="Dockerfile destination dir")
@click.option("--push", is_flag=True, help="add --push args for docker buildx")
@click.option("--force", is_flag=True, help="force build and push existed image")
@click.option(
    "--base-image", default="ghcr.io/star-whale/base:latest", help="Docker base image"
)
def _do_render(
    all: bool,
    cuda: str,
    dest_dir: str,
    push: bool,
    force: bool,
    base_image: str,
) -> None:
    cuda_candidates = [cuda]

    if all:
        cuda_candidates.extend(_CUDA_META.keys())

    if not dest_dir:
        dest_dir = os.path.join(CURRENT_DIR, ".dfs")

    dest_dir = os.path.abspath(dest_dir)

    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir, 0o755)

    tag_version = base_image.split(":")[-1]

    print(f"work @ {dest_dir} \n")
    cmds = []
    for _cuda in set(cuda_candidates):
        if not _cuda:
            continue

        for _cudnn in set([_CUDA_META[_cuda].cudnn_version, ""]):
            if _cudnn is None:
                continue

            _image_tag = _cuda
            if _cudnn:
                _image_tag = f"{_image_tag}-cudnn{_cudnn.split('.')[0]}"
            if tag_version:
                _image_tag = f"{_image_tag}-base{tag_version}"

            if not force and _check_image_existed(_image_tag):
                print(f"{_image_tag} existed, skip build and push")
                continue

            _do_render_dockerfile(_cuda, _cudnn, dest_dir, tag_version)
            cmd = _do_render_docker_buildx_cmd(
                _cuda, _cudnn, push, _image_tag, base_image, dest_dir
            )
            print(f"[done]cuda: {_cuda}, cudnn: {_cudnn if _cudnn else '--'}")
            cmds.append(cmd)

    build_fpath = os.path.join(dest_dir, "docker-build.sh")
    with open(build_fpath, "w") as f:
        f.write("#!/usr/bin/env bash \n")
        f.write("set -e \n")
        f.write("\n".join(cmds))

    print("\n run command to build docker images...")
    print(f"\t bash {build_fpath}")


def _check_image_existed(image_tag: str) -> bool:
    # TODO: check docker.io image
    image = f"{GHCR_IO_IMAGE_NAME}:{image_tag}"
    try:
        subprocess.check_call(
            ["docker", "manifest", "inspect", image],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.STDOUT,
        )
    except subprocess.CalledProcessError:
        return False
    else:
        return True


def _do_render_dockerfile(
    cuda: str,
    cudnn: str,
    dest_dir: str,
    tag_version: str,
) -> str:
    template = jinja2_env.get_template("Dockerfile.tmpl")

    kw = deepcopy(_CUDA_META[cuda]._asdict())
    kw["strike_version"] = cuda.replace(".", "-")
    kw["short_version"] = cuda
    kw["require_amd64"] = f"cuda>={cuda} {kw['require_amd64']}"
    kw["require_arm64"] = f"cuda>={cuda} {kw['require_arm64']}"
    kw["tag_version"] = tag_version

    out = template.render(**kw)
    fpath = os.path.join(dest_dir, _get_dockerfile_name(cuda, cudnn))
    with open(fpath, "w") as f:
        f.write(out)
    return fpath


def _get_dockerfile_name(cuda: str, cudnn: str) -> str:
    fname = f"Dockerfile.cuda{cuda}"
    if cudnn:
        fname = f"{fname}-cudnn{cudnn.split('.')[0]}"
    return fname


def _do_render_docker_buildx_cmd(
    cuda: str,
    cudnn: str,
    push: bool,
    image_tag: str,
    base_image: str,
    dest_dir: str,
) -> str:
    cmd = ["docker", "buildx", "build", "--platform", "linux/arm64,linux/amd64"]

    _hp = os.environ.get("HTTP_PROXY")
    if _hp:
        cmd += ["--build-arg", f"HTTP_PROXY={_hp}", "--build-arg", f"http_proxy={_hp}"]

    _hps = os.environ.get("HTTPS_PROXY")
    if _hps:
        cmd += [
            "--build-arg",
            f"HTTPS_PROXY={_hps}",
            "--build-arg",
            f"https_proxy={_hps}",
        ]

    cmd += ["--network", "host"]

    if base_image:
        cmd += ["--build-arg", f"BASE_IMAGE={base_image}"]

    cmd += [
        "-t",
        f"{GHCR_IO_IMAGE_NAME}:{image_tag}",
        "-t",
        f"{DOCKER_IO_IMAGE_NAME}:{image_tag}",
    ]

    if push:
        cmd += ["--push"]

    cmd += ["-f", os.path.join(dest_dir, _get_dockerfile_name(cuda, cudnn)), dest_dir]
    return " ".join(cmd)


if __name__ == "__main__":
    _do_render()  # type: ignore
