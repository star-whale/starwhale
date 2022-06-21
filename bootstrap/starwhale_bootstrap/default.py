import os

HOME_DIR = os.path.expanduser("~")
RECORD_LOG_DIR = HOME_DIR + "/.cache/starwhale/deploy"

# base
ROOT_PATH = "/mnt/data/starwhale"
SW_VERSION = "latest"
SW_REPOSITORY = "starwhaleai"  # or else ghcr.io/star-whale

# mysql
MYSQL_IMAGE = "mysql:8.0-debian"
MYSQL_PORT = "3406"
MYSQL_ROOT_PWD = "starwhale"
MYSQL_USER = "starwhale"
MYSQL_PWD = "starwhale"
MYSQL_DATA_DIR = "local-storage-mysql"

# minio
MINIO_IMAGE = "quay.io/minio/minio"
MINIO_DATA_DIR = "local-storage-oss"
MINIO_DEFAULT_BUCKET = "starwhale"
MINIO_ACCESS_KEY = "minioadmin"
MINIO_SECRET_KEY = "minioadmin"
MINIO_SERVER_PORT = 9000
MINIO_CONSOLE_PORT = 9001

# nexus
NEXUS_IMAGE = "sonatype/nexus3"
NEXUS_DATA_DIR = "local-storage-nexus"
NEXUS_PORT = 8081

# controller variables
CONTROLLER_IMAGE = "server:" + SW_VERSION
CONTROLLER_TASK_SPLIT_NUM = "2"
CONTROLLER_PORT = "8082"
FILE_UPLOAD_MAX_SIZE = "20480MB"
TOKEN_EXPIRE_MINUTES = "43200"

# agent variables
# agent
AGENT_IMAGE = "server:" + SW_VERSION
# task storage dir
AGENT_DATA_DIR = "agent/run"
# pypi url
PYPI_INDEX_URL = "http://10.131.0.1:3141/root/pypi-douban/+simple/"
# pypi extra url
PYPI_EXTRA_INDEX_URL = "https://pypi.tuna.tsinghua.edu.cn/simple/"
# pypi trusted host
PYPI_TRUSTED_HOST = "10.131.0.1 pypi.tuna.tsinghua.edu.cn"

# taskset
TASKSET_IMAGE = "taskset:" + SW_VERSION
TASKSET_DOCKER_PORT = "2676"
TASKSET_DIND_DIR = "agent/dind"

# hosts
HOST_OF_CONTROLLER = "controller.starwhale.com"
HOST_OF_STORAGE = "storage.starwhale.com"
HOST_OF_NEXUS = "nexus.starwhale.com"
HOST_OF_AGENT = "agent01.starwhale.com"
CLUSTER_MODE = "docker"
DEPLOY_USER = "root"
