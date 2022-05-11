from collections import namedtuple
import typing as t


class SWEnv(t.NamedTuple):
    task_id: str
    job_id: str
    status_dir: str
    log_dir: str
    result_dir: str
    input_config: str


sw_env = SWEnv(
    "SW_TASK_ID",
    "SW_JOB_ID",
    "SW_TASK_STATUS_DIR",
    "SW_TASK_LOG_DIR",
    "SW_TASK_RESULT_DIR",
    "SW_TASK_INPUT_CONFIG",
)
