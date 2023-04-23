from __future__ import annotations

import sys
import typing as t
import inspect
import numbers
import threading
from pathlib import Path
from collections import defaultdict

import yaml
from loguru import logger

from starwhale.consts import DecoratorInjectAttr
from starwhale.utils.fs import ensure_file
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.load import load_module
from starwhale.utils.error import NoSupportError
from starwhale.api._impl.evaluation import PipelineHandler


class Handler(ASDictMixin):
    _registered_handlers: t.Dict[str, Handler] = {}
    _registering_lock = threading.Lock()

    def __init__(
        self,
        name: str,
        show_name: str,
        func_name: str,
        module_name: str,
        cls_name: str = "",
        resources: t.Optional[t.Dict] = None,
        needs: t.Optional[t.List[str]] = None,
        concurrency: int = 1,
        replicas: int = 1,
        extra_args: t.Optional[t.List] = None,
        extra_kwargs: t.Optional[t.Dict] = None,
        **kw: t.Any,
    ) -> None:
        self.name = name
        self.show_name = show_name
        self.func_name = func_name
        self.module_name = module_name
        self.cls_name = cls_name
        self.resources = self._transform_resource(resources)
        self.needs = needs or []
        self.concurrency = concurrency
        self.replicas = replicas
        self.extra_args = extra_args or []
        self.extra_kwargs = extra_kwargs or {}

    def __str__(self) -> str:
        return f"Handler[{self.name}]: name-{self.show_name}"

    __repr__ = __str__

    def _transform_resource(
        self, resources: t.Optional[t.Dict[str, t.Any]]
    ) -> t.List[t.Dict]:
        resources = resources or {}
        attribute_names = ["request", "limit"]
        resource_names: t.Dict[str, t.List] = {
            "cpu": [int, float],
            "nvidia.com/gpu": [int],
            "memory": [int, float],
        }

        results = []
        for _name, _resource in resources.items():
            if _name not in resource_names:
                raise RuntimeError(
                    f"resources name is illegal, name must in {resource_names.keys()}"
                )

            if isinstance(_resource, numbers.Number):
                resources[_name] = {"request": _resource, "limit": _resource}
            elif isinstance(_resource, dict):
                if not all(n in attribute_names for n in _resource):
                    raise RuntimeError(
                        f"resources value is illegal, attribute's name must in {attribute_names}"
                    )
            else:
                raise RuntimeError(
                    "resources value is illegal, attribute's type must be number or dict"
                )

            for _k, _v in resources[_name].items():
                if type(_v) not in resource_names[_name]:
                    raise RuntimeError(
                        f"resource:{_name} only support type:{resource_names[_name]}, but now is {type(_v)}"
                    )
                if _v <= 0:
                    raise RuntimeError(
                        f"{_k} only supports non-negative number, but now is {_v}"
                    )
            results.append(
                {
                    "type": _name,
                    "request": resources[_name]["request"],
                    "limit": resources[_name]["limit"],
                }
            )
        return results

    @classmethod
    def register(
        cls,
        resources: t.Optional[t.Dict[str, t.Any]] = None,
        concurrency: int = 1,
        replicas: int = 1,
        needs: t.Optional[t.List[t.Callable]] = None,
        extra_args: t.Optional[t.List] = None,
        extra_kwargs: t.Optional[t.Dict] = None,
        name: str = "",
    ) -> t.Callable:
        """Register a function as a handler. Enable the function execute by needs handler, run with gpu/cpu/mem resources in server side,
        and control concurrency and replicas of handler run.

        Args:
            resources: [Dict, optional] Resources for the handler run, such as memory, gpu etc. Current only supports
              the cloud instance.
            concurrency: [int, optional] The concurrency of the handler run. Default is 1.
            replicas: [int, optional] The number of the handler run. Default is 1.
            needs: [List[Callable], optional] The list of the functions that need to be executed before the handler function.
              The depends callable objects must be decorated by `@handler`, `@evaluation.predict`, `@evaluation.evaluate` and `@experiment.fine_tune` .
            name: [str, optional] The user-friendly name of the handler. Default is the function name.

        Example:
        ```python
        from starwhale import handler

        @handler(resources={"cpu": 1, "nvidia.com/gpu": 1}, concurrency=2, replicas=3)
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

            _handler = cls(
                name=key_name,
                show_name=name or func_name,
                func_name=func_name,
                module_name=func.__module__,
                cls_name=cls_name,
                concurrency=concurrency,
                replicas=replicas,
                needs=key_name_needs,
                resources=resources,
                extra_args=extra_args,
                extra_kwargs=extra_kwargs,
            )

            with cls._registering_lock:
                cls._registered_handlers[key_name] = _handler

            setattr(func, DecoratorInjectAttr.Step, True)
            return func

        return decorator

    @classmethod
    def get_registered_handlers_with_expanded_needs(
        cls, search_modules: t.List[str], package_dir: Path
    ) -> t.Dict[str, t.List[Handler]]:
        cls._preload_registering_handlers(search_modules, package_dir)

        with cls._registering_lock:
            expanded_names: t.Dict[str, t.Set] = {}

            for name, handler in cls._registered_handlers.items():
                if name not in expanded_names:
                    expanded_names[name] = set()

                queue = {n for n in handler.needs}

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
                    parent_need_names = (
                        expanded_names[need_name]
                        or cls._registered_handlers[need_name].needs
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
            if not module_name:
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
                    ppl_func = getattr(v, "ppl")
                    cmp_func = getattr(v, "cmp")
                    Handler.register(replicas=2, name="ppl")(ppl_func)
                    Handler.register(
                        replicas=1,
                        needs=[ppl_func],
                        name="cmp",
                    )(cmp_func)


def generate_jobs_yaml(
    search_modules: t.List[str],
    package_dir: t.Union[Path, str],
    yaml_path: t.Union[Path, str],
) -> None:
    logger.debug(f"ingest run_handlers {search_modules} at {package_dir}")

    expanded_handlers = Handler.get_registered_handlers_with_expanded_needs(
        search_modules, Path(package_dir)
    )
    if not expanded_handlers:
        raise RuntimeError("not found any handlers")

    ensure_file(
        yaml_path,
        yaml.safe_dump(
            {
                name: [h.asdict() for h in handlers]
                for name, handlers in expanded_handlers.items()
            },
            default_flow_style=False,
        ),
        parents=True,
    )
