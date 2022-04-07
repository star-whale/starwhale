from setuptools import setup, find_packages

install_requires = [
    'click>=8.0.4',
    'importlib-metadata==4.8.2',
    'attrs==21.4.0',
    'pyyaml==6.0',
    'cattrs==1.7.1',
    'requests>=2.27.0',
    'loguru==0.6.0',
    'conda-pack==0.6.0',
    'virtualenv>=13.0.0',
    'fs>=2.4.0',
    'typing-extensions>=4.1.1',
    'commonmark>=0.9.1',
    'rich==12.0.0',
    'jsonlines==3.0.0',
    'boto3==1.21.0',
]


setup(name='starwhale',
      author='Starwhale Team',
      author_email="developer@starwhale.ai",
      version="0.1.0.dev0",
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
      swcli = starwhale.cli:cli
      sw = starwhale.cli:cli
      starwhale = starwhale.cli:cli
      """,
      python_requires = ">=3.7.0",
      scripts=[
      ],
      package_data={},
)
