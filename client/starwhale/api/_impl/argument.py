from __future__ import annotations

import typing as t
import inspect
import threading
import dataclasses
from enum import Enum
from functools import wraps
from collections import defaultdict

import click
from pydantic import BaseModel, validator
from typing_extensions import Literal

from starwhale.utils import console
from starwhale.utils.pydantic import PYDANTIC_V2


# TODO: use a more elegant way to pass extra cli args
class ExtraCliArgsRegistry:
    _args = None
    _lock = threading.Lock()

    @classmethod
    def set(cls, args: t.List[str]) -> None:
        with cls._lock:
            cls._args = args

    @classmethod
    def get(cls) -> t.List[str]:
        with cls._lock:
            return cls._args or []


# Current supported types(ref: (click types)[https://github.com/pallets/click/blob/main/src/click/types.py]):
# 1. primitive types: INT,FLOAT,BOOL,STRING
# 2. Func: FuncParamType, such as: debug: t.Union[str, t.List[DebugOption]] = dataclasses.field(default="", metadata={"help": "debug mode"})
#       we will convert FuncParamType to STRING type to simplify the input implementation. We ignore `func` field.
# 3. Choice: click.Choice type, add choices and case_sensitive options.
class OptionType(BaseModel):
    name: str
    param_type: str
    # for Choice type
    choices: t.Optional[t.List[str]] = None
    case_sensitive: bool = False

    @validator("param_type", pre=True)
    def parse_param_type(cls, value: str) -> str:
        value = value.upper()
        return "STRING" if value == "FUNC" else value


class OptionField(BaseModel):
    name: str
    opts: t.List[str]
    type: OptionType
    required: bool = False
    multiple: bool = False
    default: t.Any = None
    help: t.Optional[str] = None
    is_flag: bool = False
    hidden: bool = False


class ArgumentContext:
    _instance = None
    _lock = threading.Lock()

    def __init__(self) -> None:
        self._click_ctx = click.Context(click.Command("Starwhale Argument Decorator"))
        self._options: t.Dict[str, list] = defaultdict(list)
        self._func_related_dataclasses: t.Dict[str, list] = defaultdict(list)

    @classmethod
    def get_current_context(cls) -> ArgumentContext:
        with cls._lock:
            if cls._instance is None:
                cls._instance = ArgumentContext()
        return cls._instance

    def _key(self, o: t.Any) -> str:
        return f"{o.__module__}:{o.__qualname__}"

    def add_dataclass_type(self, func: t.Callable, dtype: t.Any) -> None:
        with self._lock:
            self._func_related_dataclasses[self._key(func)].append(self._key(dtype))

    def add_option(self, option: click.Option, dtype: t.Any) -> None:
        with self._lock:
            self._options[self._key(dtype)].append(option)

    def asdict(self) -> t.Dict[str, t.Any]:
        r: t.Dict = defaultdict(lambda: defaultdict(dict))
        for func, dtypes in self._func_related_dataclasses.items():
            for dtype in dtypes:
                for option in self._options[dtype]:
                    field = OptionField(**option.to_info_dict())
                    if PYDANTIC_V2:
                        info = field.model_dump(mode="json")
                    else:
                        info = field.dict()
                    r[func][dtype][option.name] = info
        return r

    def echo_help(self) -> None:
        if not self._options:
            click.echo("No options")
            return

        formatter = self._click_ctx.make_formatter()
        formatter.write_heading("\nOptions from Starwhale Argument Decorator")

        for group, options in self._options.items():
            help_records = []
            for option in options:
                record = option.get_help_record(self._click_ctx)
                if record:
                    help_records.append(record)

            with formatter.section(f"** {group}"):
                formatter.write_dl(help_records)

        click.echo(formatter.getvalue().rstrip("\n"))


def argument(dataclass_types: t.Any, inject_name: str = "argument") -> t.Any:
    """argument is a decorator function to define arguments for model running(predict, evaluate, serve and finetune).

    The decorated function will receive the instances of the dataclass types as the arguments.
    When the decorated function is called, the command line arguments will be parsed to the dataclass instances
    and passed to the decorated function as the keyword arguments that name is "argument".

    When use argument decorator, the decorated function must have a keyword argument named "argument" or use "**kw" keyword arguments.

    Argument:
        dataclass_types: [required] The dataclass type of the arguments.
          A list of dataclass types or a single dataclass type is supported.
        inject_name: [optional] The name of the keyword argument that will be passed to the decorated function.
          Default is "argument".

    Examples:
    ```python
    from starwhale import argument, evaluation

    @dataclass
    class EvaluationArguments:
        reshape: int = field(default=64, metadata={"help": "reshape image size"})

    @argument(EvaluationArguments)
    @evaluation.predict
    def predict_image(data, argument: EvaluationArguments):
        ...

    @argument(EvaluationArguments, inject_name="starwhale_arguments")
    @evaluation.evaluate(needs=[])
    def evaluate_summary(predict_result_iter, starwhale_arguments: EvaluationArguments):
        ...
    ```
    """
    is_sequence = True
    # TODO: support pydantic model as argument type
    if dataclasses.is_dataclass(dataclass_types):
        dataclass_types = [dataclass_types]
        is_sequence = False

    def _register_wrapper(func: t.Callable) -> t.Any:
        parser = get_parser_from_dataclasses(dataclass_types, func)
        lock = threading.Lock()
        parsed_cache: t.Any = None

        @wraps(func)
        def _run_wrapper(*args: t.Any, **kw: t.Any) -> t.Any:
            nonlocal parsed_cache, lock
            with lock:
                if parsed_cache is None:
                    parsed_cache = init_dataclasses_values(parser, dataclass_types)

            dataclass_values = parsed_cache
            if inject_name in kw:
                raise RuntimeError(
                    f"{inject_name} has been used as a keyword argument in the decorated function, please use another name by the `inject_name` option."
                )
            kw[inject_name] = dataclass_values if is_sequence else dataclass_values[0]
            return func(*args, **kw)

        return _run_wrapper

    return _register_wrapper


def init_dataclasses_values(
    parser: click.OptionParser, dataclass_types: t.Any
) -> t.Any:
    # forbid to modify the ExtraCliArgsRegistry args values
    args_map, _, params = parser.parse_args(ExtraCliArgsRegistry.get().copy())
    param_map = {p.name: p for p in params}

    ret = []
    for dtype in dataclass_types:
        keys = {f.name for f in dataclasses.fields(dtype) if f.init}
        inputs = {}
        for k, v in args_map.items():
            if k not in keys:
                continue

            # TODO: support dict type convert
            # handle multiple args for list type
            if isinstance(v, list):
                v = [param_map[k].type(i) for i in v]
            else:
                v = param_map[k].type(v)
            inputs[k] = v

        for k in inputs:
            del args_map[k]
        ret.append(dtype(**inputs))

    if args_map:
        console.warn(f"Unused args from command line: {args_map}")
    return ret


def get_parser_from_dataclasses(
    dataclass_types: t.List, deco_func: t.Callable | None = None
) -> click.OptionParser:
    argument_ctx = ArgumentContext.get_current_context()

    parser = click.OptionParser()
    for dtype in dataclass_types:
        if not dataclasses.is_dataclass(dtype):
            raise ValueError(f"{dtype} is not a dataclass type")

        if deco_func:
            argument_ctx.add_dataclass_type(func=deco_func, dtype=dtype)

        type_hints: t.Dict[str, type] = t.get_type_hints(dtype)
        for field in dataclasses.fields(dtype):
            if not field.init:
                continue
            field.type = type_hints[field.name]
            option = convert_field_to_option(field)
            option.add_to_parser(parser=parser, ctx=parser.ctx)  # type: ignore
            argument_ctx.add_option(option=option, dtype=dtype)

    parser.ignore_unknown_options = True
    return parser


def convert_field_to_option(field: dataclasses.Field) -> click.Option:
    # TODO: field.name need format for click option?
    decls = [f"--{field.name}"]
    if "_" in field.name:
        decls.append(f"--{field.name.replace('_', '-')}")
    kw: t.Dict[str, t.Any] = {
        "param_decls": decls,
        "help": field.metadata.get("help"),
        "show_default": True,
        "hidden": field.metadata.get("hidden", False),
    }

    # reference from huggingface transformers: https://github.com/huggingface/transformers/blob/main/src/transformers/hf_argparser.py
    # only support Union: NoneType(optional), str(optional) and other types, such as: Optional[int], Union[int], Union[int, str], Union[List[str], str] and Optional[Union[List[str], str]]
    origin_type = getattr(field.type, "__origin__", field.type)
    if origin_type is t.Union:
        _args = list(field.type.__args__)
        if type(None) in _args:
            _args.remove(type(None))

        _args_cnt = len(_args)
        if (_args_cnt == 2 and str not in _args) or _args_cnt > 2 or _args_cnt == 0:
            raise ValueError(
                "Only `Union[X, str, NoneType]` (i.e., `Optional[X]`) or `Union[X, str]` is allowed for `Union` because"
                " the argument parser only supports one type per argument."
                f" Problem encountered in field '{field.name}'."
            )

        if _args_cnt == 1:
            field.type = _args[0]
            origin_type = getattr(field.type, "__origin__", field.type)
        elif _args_cnt == 2:
            # filter `str` in Union
            field.type = _args[0] if _args[1] == str else _args[1]
            origin_type = getattr(field.type, "__origin__", field.type)

    if (origin_type is Literal) or (
        isinstance(field.type, type) and issubclass(field.type, Enum)
    ):
        if origin_type is Literal:
            kw["type"] = click.Choice(field.type.__args__)
        else:
            kw["type"] = click.Choice([e.value for e in field.type])

        kw["show_choices"] = True
        if field.default is not dataclasses.MISSING:
            kw["default"] = field.default
        else:
            kw["required"] = True
    elif field.type is bool:
        kw["is_flag"] = True
        kw["type"] = bool
        kw["default"] = False if field.default is dataclasses.MISSING else field.default
    elif inspect.isclass(origin_type) and issubclass(origin_type, (list, dict)):
        if issubclass(origin_type, list):
            kw["type"] = field.type.__args__[0]
            kw["multiple"] = True
        elif issubclass(origin_type, dict):
            kw["type"] = dict

        # list and dict types both need default_factory
        if field.default_factory is not dataclasses.MISSING:
            kw["default"] = field.default_factory()
        elif field.default is dataclasses.MISSING:
            kw["required"] = True
    else:
        kw["type"] = field.type
        if field.default is not dataclasses.MISSING:
            kw["default"] = field.default
        elif field.default_factory is not dataclasses.MISSING:
            kw["default"] = field.default_factory()
        else:
            kw["required"] = True

    return click.Option(**kw)
