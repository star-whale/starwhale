#  Copyright 2022 Starwhale, Inc. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import threading
from typing import Any, Dict, Callable, Optional

from starwhale.consts import DecoratorInjectAttr, DEFAULT_FINETUNE_JOB_NAME


def fine_tune(*args: Any, **kw: Any) -> Any:
    """
    This function can be used as a decorator to define a fine-tune function.

    Argument:
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.
    Examples:
    ```python
    from starwhale import experiment

    @experiment.fine_tune
    def ft():
        ...

    ```

    :return: The decorated function.
    """

    def _wrap(func: Callable) -> Any:
        _register_ft(func, **kw)
        setattr(func, DecoratorInjectAttr.FineTune, True)
        return func

    return _wrap


_registered_ft_func = threading.local()


def _register_ft(
    func: Callable,
    resources: Optional[Dict[str, Any]] = None,
) -> None:
    from .job import step

    try:
        val = _registered_ft_func.value
    except AttributeError:
        val = None

    if val is not None:
        return

    _registered_ft_func.value = step(
        job_name=DEFAULT_FINETUNE_JOB_NAME,
        name="fine-tune",
        resources=resources,
        concurrency=1,
        task_num=1,
    )(func)
