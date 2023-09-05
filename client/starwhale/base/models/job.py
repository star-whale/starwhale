from typing import List, Optional

from pydantic import BaseModel


class JobManifest(BaseModel):
    created_at: str
    scheduler_run_args: Optional[dict]
    version: str
    project: str
    model_src_dir: str
    datasets: Optional[List[str]]
    # for backward compatibility, old version may not have this field
    model: Optional[str]
    status: str
    handler_name: Optional[str]  # added from v0.5.12
    error_message: Optional[str]
    finished_at: str
