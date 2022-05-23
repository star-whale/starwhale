import os

ROOT_DIR = os.path.dirname(__file__)


def get_predefined_config_yaml() -> str:
    return open(f"{ROOT_DIR}/data/config.yaml").read()
