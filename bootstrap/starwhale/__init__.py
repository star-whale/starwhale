import click

from .cmd import bootstrap_cmd

# base todo 其他使用的地方需要配置相对路径
ROOT_PATH = '/mnt/data/starwhale'
SW_VERSION = 'latest'
SW_REPOSITORY = 'starwhaleai'  # or else ghcr.io/star-whale

# mysql
MYSQL_IMAGE = 'mysql:8.0-debian'
MYSQL_PORT = '3406'
MYSQL_ROOT_PWD = 'starwhale'
MYSQL_DATA_DIR = 'local-storage-mysql'

# minio
MINIO_IMAGE = 'quay.io/minio/minio'
MINIO_DATA_DIR = 'local-storage-oss'
MINIO_DEFAULT_BUCKET = 'starwhale'
MINIO_ACCESS_KEY = 'minioadmin'
MINIO_SECRET_KEY = 'minioadmin'
# todo the same node can't deploy multi minio instance even different port(because other port can't effect)
# reason: minio docker file expose fixed port
MINIO_SERVER_PORT = 9000
MINIO_CONSOLE_PORT = 9001

# controller variables
CONTROLLER_IMAGE = 'server:' + SW_VERSION
CONTROLLER_TASK_SPLIT_NUM = '2'
CONTROLLER_PORT = '8082'

# agent variables
# agent
AGENT_IMAGE = 'server:' + SW_VERSION
# task storage dir
AGENT_DATA_DIR = 'agent/run'
# pypi url
PYPI_INDEX_URL = 'http://10.131.0.1:3141/root/pypi-douban/+simple/'
# pypi extra url
PYPI_EXTRA_INDEX_URL = 'https://pypi.tuna.tsinghua.edu.cn/simple/'
# pypi trusted host
PYPI_TRUSTED_HOST = '10.131.0.1 pypi.tuna.tsinghua.edu.cn'

# taskset
TASKSET_IMAGE = 'taskset:' + SW_VERSION
TASKSET_DOCKER_PORT = '2676'
TASKSET_DIND_DIR = 'agent/dind'


def create_sw_bootstrap() -> click.core.Group:
    @click.group()
    def bs() -> None:
        print('hello! you are using starwhale bootstrap to deploy cluster')

    bs.add_command(bootstrap_cmd)
    return bs


bootstrap = create_sw_bootstrap()
if __name__ == "__main__":
    bootstrap()
