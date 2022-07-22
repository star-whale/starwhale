import os

HOME_DIR = os.path.expanduser("~")
RECORD_LOG_DIR = HOME_DIR + "/.cache/starwhale/deploy"

# base
ROOT_PATH = "/mnt/data/starwhale"
SW_VERSION = "latest"
SW_REPOSITORY = "starwhaleai"  # or else ghcr.io/star-whale

# mysql
MYSQL_IMAGE = "bitnami/mysql:8.0.29-debian-10-r2"
MYSQL_PORT = "3306"
MYSQL_ROOT_PWD = "starwhale"
MYSQL_USER = "starwhale"
MYSQL_PWD = "starwhale"
MYSQL_DATABASE = "starwhale"
MYSQL_DATA_DIR = "local-storage-mysql"

# minio
MINIO_IMAGE = "bitnami/minio:2022.6.20-debian-11-r0"
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
# controler storage dir
CONTROLLER_DATA_DIR = "controller"

# hosts
HOST_OF_CONTROLLER = "controller.starwhale.com"
HOST_OF_STORAGE = "storage.starwhale.com"
HOST_OF_NEXUS = "nexus.starwhale.com"
HOST_OF_AGENT = "agent01.starwhale.com"
CLUSTER_MODE = "docker"
DEPLOY_USER = "root"
