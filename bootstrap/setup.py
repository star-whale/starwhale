# Always prefer setuptools over distutils
import os
import pathlib

from setuptools import setup, find_packages

here = pathlib.Path(__file__).parent.resolve()

# Get the long description from the README file
long_description = (here / "README.md").read_text(encoding="utf-8")

install_requires = ["click", "ansible-runner", "ansible"]


def _format_version() -> str:
    _v = os.environ.get("PYPI_RELEASE_VERSION", "0.2.1.beta8")
    _v = _v.lstrip("v").replace("-", ".")
    _vs = _v.split(".", 3)
    if len(_vs) == 4:
        _vs[-1] = _vs[-1].replace(".", "")
        return ".".join(_vs)
    else:
        return _v


setup(
    name="starwhale-bootstrap",
    author="Starwhale Team",
    author_email="developer@starwhale.ai",
    version=_format_version(),
    description="MLOps Platform",
    keywords="MLOps AI",
    url="https://github.com/star-whale/starwhale",
    license="Apache-2.0",
    long_description=long_description,
    long_description_content_type="text/markdown",
    packages=find_packages(where="."),  # Required
    include_package_data=True,
    install_requires=install_requires,
    entry_points="""
      [console_scripts]
      swbs = starwhale_bootstrap:bootstrap
      starwhale-bootstrap = starwhale_bootstrap:bootstrap
      """,
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: Free for non-commercial use",
        "Operating System :: OS Independent",
    ],
)
