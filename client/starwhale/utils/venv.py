import os
import typing as t

from pathlib import Path

from subprocess import check_call


def install_req(venvdir: t.Union[str, Path], req: t.Union[str, Path]) -> None:
    #TODO: use custom pip source
    venvdir = str(venvdir)
    req = str(req)
    cmd = [os.path.join(venvdir, 'bin', 'pip'), 'install',
           '--exists-action', 'w',
           '--index-url', 'http://pypim.dapps.douban.com/simple',
           '--extra-index-url', 'https://pypi.python.org/simple/',
           '--trusted-host', 'pypim.dapps.douban.com',
           ]

    cmd += ['-r', req] if os.path.isfile(req) else [req]
    check_call(cmd)


def setup_venv(venvdir: t.Union[str, Path]) -> None:
    venvdir = str(venvdir)
    #TODO: define starwhale virtualenv.py
    check_call(['python3', '-m', 'venv', venvdir])