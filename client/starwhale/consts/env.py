from collections import namedtuple


SW_ENV = namedtuple("SW_ENV", ["TASK_ID", "JOB_ID", "STATUS_D", "LOG_D", "RESULT_D", "SWDS_CONFIG"])(
    "SW_TASK_ID", "SW_JOB_ID", "SW_TASK_STATUS_DIR", "SW_TASK_LOG_DIR", "SW_TASK_RESULT_DIR",
    "SW_TASK_SWDS_CONFIG"
)