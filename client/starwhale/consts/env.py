import typing as t


class SWEnv(t.NamedTuple):
    task_id: str = "SW_TASK_ID"
    job_id: str = "SW_JOB_ID"
    status_dir: str = "SW_TASK_STATUS_DIR"
    log_dir: str = "SW_TASK_LOG_DIR"
    result_dir: str = "SW_TASK_RESULT_DIR"
    input_config: str = "SW_TASK_INPUT_CONFIG"
