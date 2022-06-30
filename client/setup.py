import os

from setuptools import setup, find_packages

install_requires = [
    "click>=8.0.4",
    "importlib-metadata>=4.0.0",
    "attrs==21.4.0",
    "pyyaml==6.0",
    "cattrs==1.7.1",
    "requests>=2.1.0",
    "requests-toolbelt>=0.9.0",
    "loguru==0.6.0",
    "conda-pack==0.6.0",
    "virtualenv>=13.0.0",
    "fs>=2.4.0",
    "typing-extensions>=4.0.0",
    "commonmark>=0.9.1",
    "rich==12.0.0",
    "jsonlines==3.0.0",
    "boto3==1.21.0",
    "scikit-learn>=0.20.0",
    "dill==0.3.5.1",
]


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
    description="MLOps Platform",
    keywords="MLOps AI",
    url="https://github.com/star-whale/starwhale",
    license="Apache-2.0",
    packages=find_packages(exclude=["ez_setup", "tests*"]),
    include_package_data=True,
    install_requires=install_requires,
    zip_safe=False,
    entry_points="""
      [console_scripts]
      swcli = starwhale.cli:cli
      sw = starwhale.cli:cli
      starwhale = starwhale.cli:cli
      """,
    python_requires=">=3.7.0",
    scripts=[
        "scripts/sw-docker-entrypoint",
    ],
    package_data={
        "starwhale": ["swmp/templates/Dockerfile"],
    },
)
