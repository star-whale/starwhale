from __future__ import annotations

import sys
import typing as t
import inspect
import numbers
import threading
from abc import ABC, abstractmethod
from pathlib import Path
from collections import defaultdict

import yaml

from starwhale.utils import console
from starwhale.consts import DecoratorInjectAttr
from starwhale.utils.fs import ensure_file
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.load import load_module
from starwhale.utils.error import NoSupportError
from starwhale.api._impl.evaluation import PipelineHandler


class Handler(ASDictMixin):
    _registered_functions: t.Dict[str, t.Callable] = {}
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
        expose: int = 0,
        virtual: bool = False,
        require_dataset: bool = False,
        parameters_sig: t.List[t.Dict[str, object]] = [],
        ext_cmd_args: str = "",
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
        self.expose = expose
        # virtual marks that the handler is not a real user handler and can not find in the user's code
        self.virtual = virtual
        self.require_dataset = require_dataset
        self.parameters_sig = parameters_sig
        self.ext_cmd_args = ext_cmd_args

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
    def clear_registered_handlers(cls) -> None:
        with cls._registering_lock:
            cls._registered_handlers.clear()
            cls._registered_functions.clear()

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
        expose: int = 0,
        require_dataset: bool = False,
        build_in: bool = False,
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
            expose: [int, optional] The expose port of the handler. Only used for the handler run as a service.
              Default is 0. If expose is 0, there is no expose port.
              Users must set the expose port when the handler run as a service on the server or cloud instance.
            require_dataset: [bool] Whether you need datasets when execute the handler.
              Default is False, It means that there is no need to select datasets when executing this handler on the server or cloud instance.
              If True, You must select datasets when executing on the server or cloud instance.



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

            ext_cmd_args = ""
            parameters_sig = []
            #  user defined handlers i.e. not predict/evaluate/fine_tune
            if not build_in:
                sig = inspect.signature(func)
                parameters_sig = [
                    {
                        "name": p[0],
                        "required": p[1].default is inspect._empty
                        or (
                            isinstance(p[1].default, HanderInput)
                            and p[1].default.required
                        ),
                        "multiple": isinstance(p[1].default, ListInput),
                    }
                    for idx, p in enumerate(sig.parameters.items())
                    if idx != 0 or "self" != p[0]
                ]
                ext_cmd_args = " ".join(
                    [f'--{p.get("name")}' for p in parameters_sig if p.get("required")]
                )
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
                expose=expose,
                require_dataset=require_dataset,
                parameters_sig=parameters_sig,
                ext_cmd_args=ext_cmd_args,
            )

            cls._register(_handler, func)
            setattr(func, DecoratorInjectAttr.Step, True)
            import functools

            if build_in:
                return func
            else:

                @functools.wraps(func)
                def wrapper(*args: t.Any, **kwargs: t.Any) -> None:
                    if "handlerargs" in kwargs:
                        import click
                        from click.parser import OptionParser

                        handlerargs: t.List[str] = kwargs.pop("handlerargs")

                        parser = OptionParser()
                        sig = inspect.signature(func)
                        for idx, p in enumerate(sig.parameters.items()):
                            if idx != 0 or "self" != p[0]:
                                arg_name = p[0]
                                required = p[1].default is inspect._empty or (
                                    isinstance(p[1].default, HanderInput)
                                    and p[1].default.required
                                )
                                click.Option(
                                    [f"--{arg_name}", f"-{arg_name}"],
                                    is_flag=False,
                                    multiple=isinstance(p[1].default, ListInput),
                                    required=required,
                                ).add_to_parser(
                                    parser, None  # type:ignore
                                )
                        hargs, _, _ = parser.parse_args(handlerargs)

                        for idx, p in enumerate(sig.parameters.items()):
                            name = p[0]
                            if idx == 0 and "self" == name:
                                continue
                            parsed_args = {
                                name: fetch_real_args(p, hargs.get(name, None))
                            }
                            kwargs.update(
                                {k: v for k, v in parsed_args.items() if v is not None}
                            )
                    func(*args, **kwargs)

                def fetch_real_args(
                    parameter: t.Tuple[str, inspect.Parameter], user_input: t.Any
                ) -> t.Any:
                    if isinstance(parameter[1].default, HanderInput):
                        return parameter[1].default.parse(user_input)
                    else:
                        return user_input

                return wrapper

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
                    # compatible with old version: ppl and cmp function are renamed to predict and evaluate
                    predict_func = getattr(v, "predict", None) or getattr(v, "ppl")
                    evaluate_func = getattr(v, "evaluate", None) or getattr(v, "cmp")
                    Handler.register(
                        replicas=1, name="predict", require_dataset=True, build_in=True
                    )(predict_func)
                    Handler.register(
                        replicas=1,
                        needs=[predict_func],
                        name="evaluate",
                        build_in=True,
                    )(evaluate_func)

    @classmethod
    def _register(cls, handler: Handler, func: t.Callable) -> None:
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
                name: [h.asdict() for h in handlers]
                for name, handlers in expanded_handlers.items()
            },
            default_flow_style=False,
        ),
        parents=True,
    )


class HanderInput(ABC):
    def __init__(self, required: bool = False) -> None:
        self.required = required

    @abstractmethod
    def parse(self, user_input: t.Any) -> t.Any:
        ...


class ListInput(HanderInput):
    def __init__(self, member_type: t.Any, required: bool = False) -> None:
        super().__init__(required)
        self.member_type = member_type

    def parse(self, user_input: t.List) -> t.Any:
        if not user_input:
            return user_input
        if isinstance(self.member_type, HanderInput):
            return [self.member_type.parse(item) for item in user_input]
        elif issubclass(self.member_type, HanderInput):
            return [self.member_type().parse(item) for item in user_input]
        else:
            return user_input


class DatasetInput(HanderInput):
    def parse(self, user_input: t.Any) -> t.Any:
        from starwhale import dataset

        return dataset(user_input) if user_input else None


class BoolInput(HanderInput):
    def parse(self, user_input: t.Any) -> t.Any:
        return "false" != str(user_input).lower()


class IntInput(HanderInput):
    def parse(self, user_input: t.Any) -> t.Any:
        return int(user_input) if user_input else None


class FloatInput(HanderInput):
    def parse(self, user_input: t.Any) -> t.Any:
        return float(user_input) if user_input else None


class ContextInput(HanderInput):
    def parse(self, user_input: t.Any) -> t.Any:
        from starwhale import Context

        return Context.get_runtime_context()
