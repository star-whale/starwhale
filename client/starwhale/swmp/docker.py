import os
import typing as t
from collections import namedtuple
from pathlib import Path


RUNTIME = namedtuple("RUNTIME", ["PYTHON3", "STARWHALE", "PYTHON37", "PYTHON38", "PYTHON39"])(
    "python3", "starwhale", "python3.7", "python3.8", "python3.9"
)

BASE_IMAGE = namedtuple("BASE_IMAGE", ["STARWHALE", "PYTHON3", "PYTHON37", "PYTHON38"])(
    "hub.starwhale.ai/starwhale-base:0.1.0",  # Ubuntu + Python3 + Conda + other libs
    "hub.starwhale.ai/python:3", # ubuntu + 3.6 - 3.10 all in one base image
    "hub.starwhale.ai/python:3.7", # ubuntu + 3.7
    "hub.starwhale.ai/python:3.8", # ubuntu + 3.8
)


def render_dockerfile(path : t.Union[str, Path], runtime: str, base_image: str) -> None:
    pass