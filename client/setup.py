import os

from setuptools import setup, find_packages

install_requires = [
    "click-option-group==0.5.5",
    "click>=8.0.4",
    "shellingham>=1.4.0",
    # flake8 require importlib-metadata < 4.3.0
    # importlib-metadata supplies a backport of 'importlib.metadata'; Python 3.8 makes 'importlib' part of the standard lib.
    "importlib-metadata>=4.0.0, <=4.2.0;python_version < '3.8'",
    "attrs>=21.4.0",
    "pyyaml==6.0",
    "cattrs>=1.7.1",
    "requests>=2.1.0",
    "requests-toolbelt>=0.9.0",
    "loguru==0.6.0",
    "conda-pack==0.6.0",
    "virtualenv>=13.0.0",
    "fs>=2.4.0",
    "typing-extensions>=4.0.0",
    "commonmark>=0.9.1",
    "textual==0.1.18",
    "jsonlines==3.0.0",
    # botocore(>=1.29.14) fixed the cgi deprecation warning which has been updated in the boto3 1.26.14 version
    "boto3>=1.26.14",
    "scikit-learn>=0.20.0",
    # Python 3.11 needs dill >= 0.3.6
    "dill>=0.3.6",
    "packaging>=21.3",
    "pyarrow>=8.0.0",
    "Jinja2>=3.1.2",
    "tenacity>=8.0.1",
    # for system monitor
    "psutil>=5.5.0",
    "GitPython>=3.1.24",
    "filelock",
    "fastapi",
    "protobuf>=3.19.0",
    "types-protobuf>=3.19.0",
    "lz4>=3.1.10",
    "trio>=0.22.0",
    "httpx>=0.22.0",
    "urllib3<1.27",
    "pydantic<2.0.0",  # current broker and fastapi lib code only work with pydantic < 2.0.0
]

extras_require = {
    "image": ["pillow"],
    "audio": ["soundfile"],
    "serve": ["gradio", "uvicorn"],
    "online-serve": ["gradio~=3.15.0"],
}

all_requires = list(
    set(install_requires + [r for v in extras_require.values() for r in v])
)
extras_require["all"] = all_requires


def _format_version() -> str:
    _v = os.environ.get("PYPI_RELEASE_VERSION", "0.0.0.dev")
    _v = _v.lstrip("v").replace("-", ".")
    _vs = _v.split(".", 3)
    if len(_vs) == 4:
        _vs[-1] = _vs[-1].replace(".", "")
        return ".".join(_vs)
    else:
        return _v


setup(
    name="starwhale",
    author="Starwhale Team",
    author_email="developer@starwhale.ai",
    version=_format_version(),
    description="An MLOps Platform for Model Evaluation",
    long_description=open("../README.md", encoding="utf-8").read(),
    long_description_content_type="text/markdown",
    keywords="MLOps, AI, Starwhale, Model Evaluation",
    url="https://github.com/star-whale/starwhale",
    license="Apache License 2.0",
    packages=find_packages(exclude=["ez_setup", "tests*"]),
    package_data={"": ["core/runtime/template/Dockerfile.tmpl", "web/ui/**"]},
    include_package_data=True,
    install_requires=install_requires,
    extras_require=extras_require,
    zip_safe=False,
    entry_points="""
      [console_scripts]
      swcli = starwhale.cli:cli
      sw = starwhale.cli:cli
      starwhale = starwhale.cli:cli
      """,
    python_requires=">=3.7, <3.12",
    scripts=[
        "scripts/sw-docker-entrypoint",
    ],
    classifiers=[
        "Development Status :: 2 - Pre-Alpha",
        "Intended Audience :: Developers",
        "Intended Audience :: Science/Research",
        "Operating System :: OS Independent",
        "License :: OSI Approved :: Apache Software License",
        "Programming Language :: Python :: 3 :: Only",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: Implementation :: CPython",
        "Topic :: Software Development :: Libraries",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: Scientific/Engineering :: Artificial Intelligence",
        "Topic :: System :: Logging",
        "Topic :: System :: Monitoring",
    ],
)
