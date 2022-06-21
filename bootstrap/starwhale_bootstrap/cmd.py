import click
from . import default

from .deploy import deploy


@click.group("deploy", help="StarWhale Bootstrap deploy")
def bootstrap_cmd() -> None:
    pass


@bootstrap_cmd.command("run", help="run deploy script starwhale to cluster")
# common
@click.option(
    "--root-path",
    default=default.ROOT_PATH,
    help="The root directory of the runtime on the host",
)
@click.option(
    "--version",
    default=default.SW_VERSION,
    help="The product version number of this deployment",
)
@click.option(
    "--image-repository",
    default=default.SW_REPOSITORY,
    help="Docker image repository, 'starwhaleai' or 'ghcr.io/star-whale'",
)
# nexus
@click.option(
    "--need-nexus",
    default="false",
    help="whether deploy nexus",
)
@click.option(
    "--nexus-image",
    default=default.NEXUS_IMAGE,
    help="",
)
@click.option(
    "--nexus-port",
    default=default.NEXUS_PORT,
    help="",
)
@click.option(
    "--nexus-data-dir",
    default=default.NEXUS_DATA_DIR,
    help="A path relative to root-path "
    "that will as the storage directory for the nexus",
)
# mysql
@click.option(
    "--mysql-image",
    default=default.MYSQL_IMAGE,
    help="",
)
@click.option(
    "--mysql-port",
    default=default.MYSQL_PORT,
    help="",
)
@click.option(
    "--mysql-root-password",
    default=default.MYSQL_ROOT_PWD,
    help="",
)
@click.option(
    "--mysql-user",
    default=default.MYSQL_USER,
    help="",
)
@click.option(
    "--mysql-password",
    default=default.MYSQL_PWD,
    help="",
)
@click.option(
    "--mysql-data-dir",
    default=default.MYSQL_DATA_DIR,
    help="A path relative to root-path "
    "that will as the storage directory for the mysql",
)
# minio
@click.option(
    "--use-default-oss",
    default="true",
    help="THe default is minIO",
)
@click.option(
    "--oss-image",
    default=default.MINIO_IMAGE,
    help="",
)
@click.option(
    "--oss-url",
    default="http://127.0.0.1:9000",
    help="when use-default-oss is false, this option must be specified",
)
@click.option(
    "--oss-data-dir",
    default=default.MINIO_DATA_DIR,
    help="A path relative to root-path"
    " that will as the storage directory for the oss(default minio)",
)
@click.option(
    "--oss-default-bucket",
    default=default.MINIO_DEFAULT_BUCKET,
    help="",
)
@click.option(
    "--oss-access-key",
    default=default.MINIO_ACCESS_KEY,
    help="",
)
@click.option(
    "--oss-secret-key",
    default=default.MINIO_SECRET_KEY,
    help="",
)
# controller
@click.option(
    "--job-split-task-num",
    default=default.CONTROLLER_TASK_SPLIT_NUM,
    help="",
)
@click.option(
    "--controller-port",
    default=default.CONTROLLER_PORT,
    help="The port for ui and server",
)
@click.option(
    "--token-expire-minutes",
    default=default.TOKEN_EXPIRE_MINUTES,
    help="The login token expire time",
)
@click.option(
    "--file-upload-size",
    default=default.FILE_UPLOAD_MAX_SIZE,
    help="The max size of file for upload",
)
# agent
@click.option(
    "--agent-runtime-dir",
    default=default.AGENT_DATA_DIR,
    help="A path relative to root-path that will as the runtime directory for the agent",
)
# artifacts cache
@click.option(
    "--pypi-index-url",
    default=default.PYPI_INDEX_URL,
    help="",
)
@click.option(
    "--pypi-extra-index-url",
    default=default.PYPI_EXTRA_INDEX_URL,
    help="",
)
# todo add this feature
@click.option(
    "--image-registry-mirror",
    default="",
    help="",
)
@click.option(
    "--conda-channel-url",
    default="",
    help="",
)
# taskSet
@click.option(
    "--task-set-docker-port",
    default=default.TASKSET_DOCKER_PORT,
    help="The port for agent and task communication",
)
@click.option(
    "--task-set-runtime-dir",
    default=default.TASKSET_DIND_DIR,
    help="A path relative to root-path that will as the runtime directory for the taskSet",
)
# deploy environment
@click.option(
    "--user",
    default=default.DEPLOY_USER,
    help="The user who deployed the product on the host",
)
@click.option(
    "--ssh-key",
    default="",
    help="The ssh private key passed to ssh-agent " "as part of the deployment run",
)
@click.option("--log-record-dir", default=default.RECORD_LOG_DIR, help="")
@click.option("--inventory", default="", help="")
@click.option(
    "--host-of-controller",
    default=default.HOST_OF_CONTROLLER,
    help="Only one,the controller host",
)
@click.option(
    "--host-of-storage",
    default=default.HOST_OF_STORAGE,
    help="Only one,the storage host",
)
@click.option(
    "--host-of-nexus",
    default=default.HOST_OF_NEXUS,
    help="Only one,the nexus host",
)
@click.option(
    "--hosts-of-agent",
    default=default.HOST_OF_AGENT,
    # multiple=True,
    # cls=PythonLiteralOption,
    help="At least one exists, in the format is 'agent[**].starwhale.com'",
)
# common
@click.option(
    "--cluster-mode",
    default=default.CLUSTER_MODE,
    help="The mode of the cluster, which can be docker or k8s",
)
def _deploy(
    root_path: str,
    version: str,
    image_repository: str,
    token_expire_minutes: str,
    file_upload_size: str,
    need_nexus: bool,
    nexus_image: str,
    nexus_port: str,
    nexus_data_dir: str,
    mysql_image: str,
    mysql_port: str,
    mysql_root_password: str,
    mysql_user: str,
    mysql_password: str,
    mysql_data_dir: str,
    use_default_oss: bool,
    oss_image: str,
    oss_url: str,
    oss_data_dir: str,
    oss_default_bucket: str,
    oss_access_key: str,
    oss_secret_key: str,
    job_split_task_num: int,
    controller_port: int,
    agent_runtime_dir: str,
    pypi_index_url: str,
    pypi_extra_index_url: str,
    image_registry_mirror: str,
    conda_channel_url: str,
    task_set_docker_port: int,
    task_set_runtime_dir: str,
    user: str,
    ssh_key: str,
    log_record_dir: str,
    inventory: str,
    host_of_controller: str,
    host_of_storage: str,
    host_of_nexus: str,
    hosts_of_agent: str,
    cluster_mode: str,
) -> None:
    agent_hosts = {}
    for agent in hosts_of_agent.strip().split(","):
        agent_hosts[agent] = {}

    inventory = {
        "controller": {"hosts": {host_of_controller: {}}},
        "storage": {"hosts": {host_of_storage: {}}},
        "agent": {"hosts": agent_hosts},
    }
    if need_nexus:
        inventory["nexus"] = {"hosts": {host_of_nexus: {}}}

    deploy(
        log_record_dir,
        {
            # base
            "base_root_path": root_path,
            "sw_version": version,
            "sw_repository": image_repository,  # or else ghcr.io/star-whale
            # nexus
            "need_nexus": need_nexus,
            "nexus_image": nexus_image,
            "nexus_port": nexus_port,
            "nexus_data_dir": "{{ base_root_path }}/" + nexus_data_dir,
            # mysql
            "mysql_image": mysql_image,
            "mysql_port": mysql_port,
            "mysql_root_pwd": mysql_root_password,
            "mysql_user": mysql_user,
            "mysql_pwd": mysql_password,
            "mysql_data_dir": "{{ base_root_path }}/" + mysql_data_dir,
            # minio
            "minio_image": oss_image,
            "minio_data_dir": "{{ base_root_path }}/" + oss_data_dir,
            "minio_default_bucket": oss_default_bucket,
            "minio_access_key": oss_access_key,
            "minio_secret_key": oss_secret_key,
            # reason: minio docker file expose fixed port
            "minio_server_port": "9000",
            "minio_console_port": "9001",
            # controller variables
            "controller_image": "{{ sw_repository }}/server:{{ sw_version }}",
            "controller_task_split_num": job_split_task_num,
            "controller_port": controller_port,
            "token_expire_minutes": token_expire_minutes,
            "file_upload_size": file_upload_size,
            # agent variables
            # agent
            "agent_image": "{{ sw_repository }}/server:{{ sw_version }}",
            "task_default_image": "{{ sw_repository }}/starwhale:latest",
            # task storage dir
            "agent_data_dir": "{{ base_root_path }}/" + agent_runtime_dir,
            # pypi url
            "pypi_index_url": pypi_index_url,
            # pypi extra url
            "pypi_extra_index_url": pypi_extra_index_url,
            # pypi trusted host
            "pypi_trusted_host": "10.131.0.1 pypi.tuna.tsinghua.edu.cn",
            # taskset
            "taskset_image": "{{ sw_repository }}/taskset:{{ sw_version }}",
            "taskset_docker_port": task_set_docker_port,
            "taskset_dind_dir": "{{ base_root_path }}/" + task_set_runtime_dir,
        },
        "--user " + user,
        inventory,
    )
