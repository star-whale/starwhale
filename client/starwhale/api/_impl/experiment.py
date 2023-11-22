from __future__ import annotations

import os
import time
import typing as t
from pathlib import Path
from functools import wraps, partial

from starwhale.utils import console
from starwhale.consts import SHORT_VERSION_CNT, DecoratorInjectAttr
from starwhale.base.context import Context
from starwhale.api._impl.model import build as build_starwhale_model
from starwhale.api._impl.dataset import Dataset
from starwhale.base.client.models.models import FineTune


# TODO: support arguments
# TODO: support model exclude_patterns
def finetune(*args: t.Any, **kw: t.Any) -> t.Any:
    """
    This function can be used as a decorator to define a fine-tune function.

    Argument:
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.
        needs: [List[Callable], optional] The list of functions that the fine-tune function depends on.
        require_train_datasets: [bool, optional] Whether the fine-tune function requires train datasets. Default is True.
            When the argument is True, the fine-tune function will receive the train datasets(List[Dataset] type) as the first argument.
        require_validation_datasets: [bool, optional] Whether the fine-tune function requires validation datasets. Default is False.
            When the argument is True, the fine-tune function will receive the validation datasets(List[Dataset] type)
            as the second argument(if require_train_datasets=True) or as the first argument(if require_train_datasets=False).
        auto_build_model: [bool, optional] Whether to automatically build the starwhale model. Default is True.
        model_modules: [List[str|object], optional] The search models for model building.   Default is None.
            The search modules supports object(function, class or module) or str(example: "to.path.module", "to.path.module:object").
            If the argument is not specified, the search modules are the imported modules.

    Examples:
    ```python
    from starwhale import finetune

    @finetune
    def ft(train_datasets, validation_datasets):
        ...

    @finetune(resources={"nvidia.com/gpu": 1}, needs=[prepare_handler], require_validation_datasets=False)
    def ft(train_datasets):
        ...

    ```

    Returns:
        The decorated function
    """

    if len(args) == 1 and len(kw) == 0 and callable(args[0]):
        return finetune()(args[0])
    else:
        require_train_datasets = kw.get("require_train_datasets", True)
        require_validation_datasets = kw.get("require_validation_datasets", False)
        auto_build_model = kw.get("auto_build_model", True)
        model_modules = kw.get("model_modules")
        workdir = Path.cwd()

        def _register_wrapper(func: t.Callable) -> t.Any:
            _register_ft(
                func=func,
                resources=kw.get("resources"),
                needs=kw.get("needs"),
                require_train_datasets=require_train_datasets,
                require_validation_datasets=require_validation_datasets,
                auto_build_model=auto_build_model,
            )

            @wraps(func)
            def _run_wrapper(*args: t.Any, **kw: t.Any) -> t.Any:
                ctx = Context.get_runtime_context()
                load_dataset = partial(Dataset.dataset, readonly=True, create="forbid")

                inject_args = []
                if require_train_datasets:
                    if not ctx.dataset_uris:
                        raise RuntimeError(
                            "train datasets are required, use `--dataset` to specify the train datasets in `swcli model run` command."
                        )
                    inject_args.append([load_dataset(uri) for uri in ctx.dataset_uris])
                if require_validation_datasets:
                    if not ctx.finetune_val_dataset_uris:
                        raise RuntimeError(
                            "validation datasets are required, use `--val-dataset` to specify the validation datasets in `swcli model run` command."
                        )
                    inject_args.append(
                        [load_dataset(uri) for uri in ctx.finetune_val_dataset_uris]
                    )

                # TODO: support arguments from command line
                ret = func(*inject_args)

                if auto_build_model:
                    console.info(f"building starwhale model package from {workdir}")
                    # The finetune tag is exclusively used for display purposes.
                    tag = f"finetune-{ctx.version[:SHORT_VERSION_CNT]}-{time.time_ns()}"
                    build_starwhale_model(
                        name=os.environ.get("SW_FINETUNE_TARGET_MODEL", ctx.model_name),
                        modules=model_modules,
                        workdir=workdir,
                        tags=[tag],
                    )

                return ret

            return _run_wrapper

    return _register_wrapper


def _register_ft(
    func: t.Callable,
    resources: t.Optional[t.Dict[str, t.Any]] = None,
    needs: t.Optional[t.List[t.Callable]] = None,
    require_train_datasets: bool = True,
    require_validation_datasets: bool = False,
    auto_build_model: bool = True,
) -> None:
    from .job import Handler

    Handler.register(
        name="finetune",
        resources=resources,
        replicas=1,
        needs=needs,
        require_dataset=require_train_datasets or require_validation_datasets,
        extra_kwargs=dict(
            auto_build_model=auto_build_model,
        ),
        built_in=True,
        fine_tune=FineTune(
            require_train_datasets=require_train_datasets,
            require_validation_datasets=require_validation_datasets,
        ),
    )(func)
    setattr(func, DecoratorInjectAttr.FineTune, True)
