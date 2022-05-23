import typing as t


class InstanceType:
    STANDALONE = "standalone"
    CLOUD = "cloud"


class Instance(object):
    def __init__(self, typ: str, uri: str) -> None:
        pass
