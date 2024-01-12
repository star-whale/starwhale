from __future__ import annotations

import sys
import typing as t
import inspect
import threading
from pathlib import Path
from functools import partial
from collections import defaultdict

import yaml

from starwhale.utils import console, convert_to_bytes
from starwhale.consts import DecoratorInjectAttr
from starwhale.utils.fs import ensure_file
from starwhale.utils.load import load_module
from starwhale.utils.error import NoSupportError
from starwhale.base.models.model import StepSpecClient
from starwhale.api._impl.evaluation import PipelineHandler
from starwhale.base.client.models.models import FineTune, RuntimeResource


class Handler:
    _registered_functions: t.Dict[str, t.Callable] = {}
    _registered_handlers: t.Dict[str, StepSpecClient] = {}
    _registering_lock = threading.Lock()

    def __str__(self) -> str:
        return f"Handler {self._registered_handlers.keys()}"

    __repr__ = __str__

    @staticmethod
    def _transform_resource(
        resources: t.Optional[t.Dict[str, t.Any]]
    ) -> t.List[RuntimeResource]:
        if not resources:
            return []
        resources = resources or {}
        attribute_names = ["request", "limit"]
        resource_names: t.Dict[str, t.List] = {
            "cpu": [int, float],
            "nvidia.com/gpu": [int],
            "memory": [int, float, str],
        }

        results = []
        for _name, _resource in resources.items():
            if _name not in resource_names:
                raise RuntimeError(
                    f"resources name is illegal, name must in {resource_names.keys()}"
                )

            if isinstance(_resource, dict):
                if not all(n in attribute_names for n in _resource):
                    raise RuntimeError(
                        f"resources value is illegal, attribute's name must in {attribute_names}"
                    )
            else:
                resources[_name] = {"request": _resource, "limit": _resource}

            for _k, _v in resources[_name].items():
                if type(_v) not in resource_names[_name]:
                    raise RuntimeError(
                        f"resource:{_name} only support type:{resource_names[_name]}, but now is {type(_v)}"
                    )

                if isinstance(_v, (int, float)) and _v < 0:
                    raise RuntimeError(
                        f"{_k} only supports non-negative number, but now is {_v}"
                    )

                if _name == "memory" and isinstance(_v, str):
                    resources[_name][_k] = convert_to_bytes(_v)

            rc = RuntimeResource(
                type=_name,
                request=resources[_name]["request"],
                limit=resources[_name]["limit"],
            )
            results.append(rc)
        return results

    @classmethod
    def clear_registered_handlers(cls) -> None:
        with cls._registering_lock:
            cls._registered_handlers.clear()
            cls._registered_functions.clear()

    @classmethod
    def register(
        cls,
        resources: t.Dict[str, t.Any] | None = None,
        replicas: int = 1,
        needs: t.List[t.Callable] | None = None,
        extra_args: t.List | None = None,
        extra_kwargs: t.Dict | None = None,
        name: str = "",
        expose: int = 0,
        require_dataset: bool = False,
        fine_tune: FineTune | None = None,
    ) -> t.Callable:
        """Register a function as a handler. Enable the function execute by needs handler, run with gpu/cpu/mem resources in server side,
        and control replicas of handler run.

        Arguments:
            resources: [Dict, optional] Resources for the handler run, such as memory, cpu, nvidia.com/gpu etc. Current only supports
              the server instance.
            replicas: [int, optional] The number of the handler run. Default is 1.
            needs: [List[Callable], optional] The list of the functions that need to be executed before the handler function.
              The depends callable objects must be decorated by `@handler`, `@evaluation.predict`, `@evaluation.evaluate` and `@experiment.fine_tune` .
            name: [str, optional] The user-friendly name of the handler. Default is the function name.
            expose: [int, optional] The expose port of the handler. Only used for the handler run as a service.
              Default is 0. If expose is 0, there is no expose port.
              Users must set the expose port when the handler run as a service on the server or cloud instance.
            require_dataset: [bool, optional] Whether you need datasets when execute the handler.
              Default is False, It means that there is no need to select datasets when executing this handler on the server or cloud instance.
              If True, You must select datasets when executing on the server or cloud instance.
            fine_tune: [FineTune, optional The fine tune config for the handler. Default is None.

        Example:
        ```python
        from starwhale import handler

        @handler(resources={"cpu": 1, "nvidia.com/gpu": 1}, replicas=3)
        def my_handler():
            ...

        @handler(needs=[my_handler])
        def my_another_handler():
            ...
        ```
        Returns:
            [Callable] The decorator function.
        """

        def decorator(func: t.Callable) -> t.Callable:
            if not inspect.isfunction(func):
                raise NoSupportError(
                    f"handler decorator only supports on function: {func}"
                )

            qualname = func.__qualname__
            cls_name, _, func_name = qualname.rpartition(".")
            if "." in cls_name:
                raise NoSupportError(
                    f"handler decorator no supports inner class method:{qualname}"
                )

            key_name = f"{func.__module__}:{qualname}"
            key_name_needs = []
            for n in needs or []:
                # TODO: support class as needs
                if not inspect.isfunction(n):
                    raise NoSupportError(
                        f"handler decorator no supports non-function needs:{n}"
                    )

                key_name_needs.append(f"{n.__module__}:{n.__qualname__}")

            # TODO: check arguments, then dump for Starwhale Console
            _handler = StepSpecClient(
                name=key_name,
                show_name=name or func_name,
                func_name=func_name,
                module_name=func.__module__,
                cls_name=cls_name,
                replicas=replicas,
                needs=key_name_needs,
                resources=cls._transform_resource(resources),
                extra_args=extra_args,
                extra_kwargs=extra_kwargs,
                expose=expose,
                require_dataset=require_dataset,
                fine_tune=fine_tune,
            )

            cls._register(_handler, func)
            setattr(func, DecoratorInjectAttr.Step, True)
            return func

        return decorator

    @classmethod
    def get_registered_handlers_with_expanded_needs(
        cls, search_modules: t.List[str], package_dir: Path
    ) -> t.Dict[str, t.List[StepSpecClient]]:
        cls._preload_registering_handlers(search_modules, package_dir)

        with cls._registering_lock:
            expanded_names: t.Dict[str, t.Set] = {}

            from starwhale.api._impl.argument import ArgumentContext

            ctx = ArgumentContext.get_current_context()
            handler_args = ctx.asobj()
            for name, handler in cls._registered_handlers.items():
                handler.arguments = handler_args.get(handler.name)  # type: ignore[assignment]

                if name not in expanded_names:
                    expanded_names[name] = set()

                queue = {n for n in handler.needs or []}

                while queue:
                    need_name = queue.pop()

                    if need_name in expanded_names[name]:
                        continue

                    if need_name == name:
                        raise RuntimeError(
                            f"cycle dependency: name-{name}, needs-{handler.needs}"
                        )

                    if need_name not in cls._registered_handlers:
                        raise RuntimeError(f"dependency not found: {need_name}")

                    expanded_names[name].add(need_name)
                    parent_need_names = expanded_names[need_name] or set(
                        cls._registered_handlers[need_name].needs or []
                    )
                    queue.update(parent_need_names)

            expanded_handlers = defaultdict(list)
            for name, need_names in expanded_names.items():
                expanded_handlers[name].extend(
                    [cls._registered_handlers[n] for n in need_names]
                )
                expanded_handlers[name].append(cls._registered_handlers[name])

            return expanded_handlers

    @classmethod
    def _preload_registering_handlers(
        cls, search_modules: t.List[str], package_dir: Path
    ) -> None:
        for module_name in search_modules:
            # handler format: a.b.c, a.b.c:d
            module_name = module_name.split(":")[0].strip()
            if not module_name or module_name == "__main__":
                continue

            # reload for the multi model.build in one python process
            if module_name in sys.modules:
                del sys.modules[module_name]
            module = load_module(module_name, package_dir)

            for v in module.__dict__.values():
                if (
                    inspect.isclass(v)
                    and issubclass(v, PipelineHandler)
                    and v != PipelineHandler
                ):
                    _cls = v
                    # compatible with old version: ppl and cmp function are renamed to predict and evaluate
                    predict_func = getattr(v, "predict", None) or getattr(v, "ppl")
                    evaluate_func = getattr(v, "evaluate", None) or getattr(v, "cmp")

                    predict_register = partial(
                        Handler.register,
                        name="predict",
                        require_dataset=True,
                    )

                    evaluate_register = partial(
                        Handler.register,
                        needs=[predict_func],
                        name="evaluate",
                        replicas=1,
                    )

                    run_info = getattr(_cls, "_registered_run_info", None)
                    if run_info:
                        predict_run_kw: t.Dict = {"replicas": 1}
                        evaluate_run_kw: t.Dict = {"replicas": 1}

                        for k, v in run_info.items():
                            _cls_name, _, _func_name = k.rpartition(".")
                            if _cls_name != _cls.__name__:
                                continue

                            if _func_name == "predict":
                                predict_run_kw.update(v)
                            elif _func_name == "evaluate":
                                evaluate_run_kw.update(v)

                        predict_register(
                            replicas=predict_run_kw.get("replicas", 1),
                            resources=predict_run_kw.get("resources"),
                        )(predict_func)
                        evaluate_register(resources=evaluate_run_kw.get("resources"))(
                            evaluate_func
                        )
                    else:
                        predict_register(replicas=1)(predict_func)
                        evaluate_register()(evaluate_func)

    @classmethod
    def _register(cls, handler: StepSpecClient, func: t.Callable) -> None:
        with cls._registering_lock:
            cls._registered_handlers[handler.name] = handler
            cls._registered_functions[handler.name] = func


def generate_jobs_yaml(
    search_modules: t.List[str],
    package_dir: t.Union[Path, str],
    yaml_path: t.Union[Path, str],
) -> None:
    console.print(
        f":rocket: generate jobs yaml from modules: {search_modules} , package rootdir: {package_dir}"
    )
    expanded_handlers = Handler.get_registered_handlers_with_expanded_needs(
        search_modules, Path(package_dir)
    )
    if not expanded_handlers:
        raise RuntimeError("not found any handlers")

    ensure_file(
        yaml_path,
        yaml.safe_dump(
            {
                name: [h.to_dict() for h in handlers]
                for name, handlers in expanded_handlers.items()
            },
            default_flow_style=False,
        ),
        parents=True,
    )
