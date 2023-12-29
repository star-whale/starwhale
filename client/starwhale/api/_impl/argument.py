from __future__ import annotations

import typing as t
import inspect
import threading
import dataclasses
from enum import Enum
from functools import wraps
from collections import defaultdict

import click

from starwhale.utils import console


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


class ArgumentContext:
    _instance = None
    _lock = threading.Lock()

    def __init__(self) -> None:
        self._click_ctx = click.Context(click.Command("Starwhale Argument Decorator"))
        self._options: t.Dict[str, list] = defaultdict(list)

    @classmethod
    def get_current_context(cls) -> ArgumentContext:
        with cls._lock:
            if cls._instance is None:
                cls._instance = ArgumentContext()
        return cls._instance

    def add_option(self, option: click.Option, group: str) -> None:
        with self._lock:
            self._options[group].append(option)

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
    if dataclasses.is_dataclass(dataclass_types):
        dataclass_types = [dataclass_types]
        is_sequence = False

    def _register_wrapper(func: t.Callable) -> t.Any:
        # TODO: dump parser to json file when model building
        parser = get_parser_from_dataclasses(dataclass_types)

        @wraps(func)
        def _run_wrapper(*args: t.Any, **kw: t.Any) -> t.Any:
            dataclass_values = init_dataclasses_values(parser, dataclass_types)
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
    args_map, _, params = parser.parse_args(ExtraCliArgsRegistry.get())
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


def get_parser_from_dataclasses(dataclass_types: t.Any) -> click.OptionParser:
    argument_ctx = ArgumentContext.get_current_context()
    parser = click.OptionParser()
    for dtype in dataclass_types:
        if not dataclasses.is_dataclass(dtype):
            raise ValueError(f"{dtype} is not a dataclass type")

        type_hints: t.Dict[str, type] = t.get_type_hints(dtype)
        for field in dataclasses.fields(dtype):
            if not field.init:
                continue
            field.type = type_hints[field.name]
            option = convert_field_to_option(field)
            option.add_to_parser(parser=parser, ctx=parser.ctx)  # type: ignore
            argument_ctx.add_option(
                option=option, group=f"{dtype.__module__}.{dtype.__qualname__}"
            )

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
    # only support: Union[xxx, None] or Union[EnumType, str] or [List[EnumType], str] type
    origin_type = getattr(field.type, "__origin__", field.type)
    if origin_type is t.Union:
        if (
            str not in field.type.__args__ and type(None) not in field.type.__args__
        ) or (len(field.type.__args__) != 2):
            raise ValueError(
                f"{field.type} is not supported."
                "Only support Union[xxx, None] or Union[EnumType, str] or [List[EnumType], str] type"
            )

        if type(None) in field.type.__args__:
            # ignore None type, use another type as the field type
            field.type = (
                field.type.__args__[0]
                if field.type.__args__[1] == type(None)
                else field.type.__args__[1]
            )
            origin_type = getattr(field.type, "__origin__", field.type)
        else:
            # ignore str and None type, use another type as the field type
            field.type = (
                field.type.__args__[0]
                if field.type.__args__[1] == str
                else field.type.__args__[1]
            )
            origin_type = getattr(field.type, "__origin__", field.type)

    try:
        # typing.Literal is only supported in python3.8+
        literal_type = t.Literal  # type: ignore[attr-defined]
    except AttributeError:
        literal_type = None

    if (literal_type and origin_type is literal_type) or (
        isinstance(field.type, type) and issubclass(field.type, Enum)
    ):
        if literal_type and origin_type is literal_type:
            kw["type"] = click.Choice(field.type.__args__)
        else:
            kw["type"] = click.Choice([e.value for e in field.type])

        kw["show_choices"] = True
        if field.default is not dataclasses.MISSING:
            kw["default"] = field.default
        else:
            kw["required"] = True
    elif field.type is bool or field.type == t.Optional[bool]:
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
