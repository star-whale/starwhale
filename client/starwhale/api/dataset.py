from ._impl.dataset import BuildExecutor, MNISTBuildExecutor

#TODO: add dataset build/push/list/info api

__all__ = ['list', 'push', 'info', 'build',
           'BuildExecutor', 'MNISTBuildExecutor']


def list(filter: str):
    pass


def push(swds: str):
    pass


def info(swds: str):
    pass


def build():
    pass