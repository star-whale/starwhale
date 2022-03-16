from setuptools import setup, find_packages

install_requires = open("requirements.txt").readlines()


setup(name='starwhale',
      version="0.1.0",
      description='MLOps Platform',
      keywords="MLOps AI",
      url='https://github.com/star-whale/starwhale',
      license='Apache-2.0',
      packages=find_packages(exclude=['ez_setup', 'tests*']),
      include_package_data=True,
      install_requires=install_requires,
      zip_safe=False,
      entry_points="""
      [console_scripts]
      starwhale = starwhale.cli:cli
      sw = starwhale.cli:cli
      """,
      python_requires = ">=3.7.0",
      scripts=[
      ],
      package_data={
      },
)
