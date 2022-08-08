from setuptools import setup, find_packages

setup(
    name="dummy",
    description="Dummy package only for test",
    version="0.0.0",
    packages=find_packages(exclude=["ez_setup", "tests*"]),
    include_package_data=True,
    entry_points="""
        [console_scripts]
        dummy=dummy:cli
    """,
)
