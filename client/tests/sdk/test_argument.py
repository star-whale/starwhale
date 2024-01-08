from __future__ import annotations

import json
import typing as t
import dataclasses
from enum import Enum
from unittest.mock import patch, MagicMock

import click
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.api._impl.argument import argument as argument_decorator
from starwhale.api._impl.argument import (
    ArgumentContext,
    ExtraCliArgsRegistry,
    get_parser_from_dataclasses,
)


class IntervalStrategy(Enum):
    NO = "no"
    STEPS = "steps"
    EPOCH = "epoch"


class DebugOption(Enum):
    UNDERFLOW_OVERFLOW = "underflow_overflow"
    TPU_METRICS_DEBUG = "tpu_metrics_debug"


class FSDPOption(Enum):
    FSDP = "fsdp"
    FSDP2 = "fsdp2"


@dataclasses.dataclass
class ScalarArguments:
    no_field = 1
    batch: int = dataclasses.field(default=64, metadata={"help": "batch size"})
    overwrite: bool = dataclasses.field(default=False, metadata={"help": "overwrite"})
    learning_rate: float = dataclasses.field(
        default=0.01, metadata={"help": "learning rate"}
    )
    half_precision_backend: str = dataclasses.field(
        default="auto", metadata={"help": "half precision backend"}
    )
    epoch: int = dataclasses.field(default_factory=lambda: 1)


@dataclasses.dataclass
class ComposeArguments:
    # simply huggingface transformers TrainingArguments for test
    debug: t.Union[str, t.List[DebugOption]] = dataclasses.field(
        default="", metadata={"help": "debug mode"}
    )

    lr_scheduler_kwargs: t.Optional[t.Dict] = dataclasses.field(
        default_factory=dict, metadata={"help": "lr scheduler kwargs"}
    )
    evaluation_strategy: t.Union[IntervalStrategy, str] = dataclasses.field(
        default="no", metadata={"help": "evaluation strategy"}
    )
    per_gpu_train_batch_size: t.Optional[int] = dataclasses.field(default=None)
    eval_delay: t.Optional[float] = dataclasses.field(
        default=0, metadata={"help": "evaluation delay"}
    )
    label_names: t.Optional[t.List[str]] = dataclasses.field(
        default=None, metadata={"help": "label names"}
    )
    fsdp: t.Optional[t.Union[t.List[FSDPOption], str]] = dataclasses.field(
        default="", metadata={"help": "fsdp"}
    )
    fsdp2: t.Optional[t.Union[str, t.List[FSDPOption]]] = dataclasses.field(
        default="", metadata={"help": "fsdp2"}
    )
    tf32: t.Optional[bool] = dataclasses.field(default=None, metadata={"help": "tf32"})


class ArgumentTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def tearDown(self) -> None:
        ExtraCliArgsRegistry._args = None
        ArgumentContext._instance = None

    def test_argument_exceptions(self) -> None:
        @argument_decorator(ScalarArguments)
        def no_argument_func():
            ...

        @argument_decorator(ScalarArguments)
        def argument_keyword_func(argument):
            ...

        with self.assertRaisesRegex(TypeError, "got an unexpected keyword argument"):
            no_argument_func()

        with self.assertRaisesRegex(
            RuntimeError,
            "has been used as a keyword argument in the decorated function",
        ):
            argument_keyword_func(argument=1)

    def test_argument_decorator(self) -> None:
        @argument_decorator(
            (ScalarArguments, ComposeArguments), inject_name="starwhale_argument"
        )
        def assert_func(starwhale_argument: t.Tuple) -> None:
            scalar_argument, compose_argument = starwhale_argument
            assert isinstance(scalar_argument, ScalarArguments)
            assert isinstance(compose_argument, ComposeArguments)

            assert scalar_argument.batch == 128
            assert scalar_argument.overwrite is True
            assert scalar_argument.learning_rate == 0.02
            assert scalar_argument.half_precision_backend == "auto"
            assert scalar_argument.epoch == 1

            assert compose_argument.label_names == ["a", "b", "c"]
            assert compose_argument.eval_delay == 0
            assert compose_argument.per_gpu_train_batch_size == 8
            assert compose_argument.evaluation_strategy == "steps"
            assert compose_argument.debug == [DebugOption.UNDERFLOW_OVERFLOW]

        ExtraCliArgsRegistry.set(
            [
                "--batch",
                "128",
                "--overwrite",
                "--learning-rate=0.02",
                "--debug",
                "underflow_overflow",
                "--evaluation_strategy",
                "steps",
                "--per_gpu_train_batch_size",
                "8",
                "--label_names",
                "a",
                "--label_names",
                "b",
                "--label_names",
                "c",
                "--no-defined-arg=1",
            ]
        )
        assert_func()

    def test_parser_exceptions(self) -> None:
        with self.assertRaisesRegex(ValueError, "is not a dataclass type"):
            get_parser_from_dataclasses([None])

    def test_scalar_parser(self) -> None:
        scalar_parser = get_parser_from_dataclasses([ScalarArguments])
        assert scalar_parser.ignore_unknown_options

        assert "--no_field" not in scalar_parser._long_opt

        batch = scalar_parser._long_opt["--batch"].obj
        assert batch.type == click.INT
        assert not batch.required
        assert batch.help == "batch size"
        assert not batch.is_flag
        assert batch.default == 64
        overwrite = scalar_parser._long_opt["--overwrite"].obj
        assert overwrite.type == click.BOOL
        assert overwrite.is_flag
        assert overwrite.default is False
        assert scalar_parser._long_opt["--learning-rate"].obj.type == click.FLOAT
        assert (
            scalar_parser._long_opt["--half_precision_backend"].obj.type == click.STRING
        )
        assert scalar_parser._long_opt["--epoch"].obj.type == click.INT
        assert scalar_parser._long_opt["--epoch"].obj.default == 1

        argument_ctx = ArgumentContext.get_current_context()
        assert len(argument_ctx._options) == 1
        options = argument_ctx._options["tests.sdk.test_argument:ScalarArguments"]
        assert len(options) == 5
        assert options[0].name == "batch"
        assert options[-1].name == "epoch"
        argument_ctx.echo_help()

    def test_compose_parser(self) -> None:
        compose_parser = get_parser_from_dataclasses([ComposeArguments])

        dict_obj = compose_parser._long_opt["--lr-scheduler-kwargs"].obj
        assert not dict_obj.required
        assert dict_obj.default == {}
        assert not dict_obj.multiple
        assert isinstance(dict_obj.type, click.types.FuncParamType)
        assert dict_obj.type.func == dict

        union_enum_obj = compose_parser._long_opt["--evaluation_strategy"].obj
        assert not union_enum_obj.required
        assert union_enum_obj.default == "no"
        assert isinstance(union_enum_obj.type, click.Choice)
        assert union_enum_obj.type.choices == ["no", "steps", "epoch"]
        assert union_enum_obj.show_choices
        assert not union_enum_obj.multiple

        union_list_obj = compose_parser._long_opt["--debug"].obj
        assert isinstance(union_list_obj.type, click.types.FuncParamType)
        assert union_list_obj.type.func == DebugOption
        assert not union_list_obj.required
        assert union_list_obj.default is None
        assert union_list_obj.multiple

        optional_int_obj = compose_parser._long_opt["--per_gpu_train_batch_size"].obj
        assert optional_int_obj.type == click.INT
        assert not optional_int_obj.required
        assert optional_int_obj.default is None
        assert not optional_int_obj.multiple

        optional_float_obj = compose_parser._long_opt["--eval_delay"].obj
        assert optional_float_obj.type == click.FLOAT
        assert not optional_float_obj.required
        assert optional_float_obj.default == 0
        assert not optional_float_obj.multiple

        optional_list_obj = compose_parser._long_opt["--label_names"].obj
        assert optional_list_obj.type == click.STRING
        assert not optional_list_obj.required
        assert optional_list_obj.multiple
        assert optional_list_obj.default is None

        fsdp_obj = compose_parser._long_opt["--fsdp"].obj
        assert isinstance(fsdp_obj.type, click.types.FuncParamType)
        assert fsdp_obj.type.func == FSDPOption

        fsdp_obj2 = compose_parser._long_opt["--fsdp2"].obj
        assert fsdp_obj2.type.func == fsdp_obj.type.func

        tf32_obj = compose_parser._long_opt["--tf32"].obj
        assert tf32_obj.type == click.BOOL

        argument_ctx = ArgumentContext.get_current_context()
        assert len(argument_ctx._options) == 1
        options = argument_ctx._options["tests.sdk.test_argument:ComposeArguments"]
        assert len(options) == 9
        assert options[0].name == "debug"
        argument_ctx.echo_help()

    @patch("click.echo")
    def test_argument_help_output(self, mock_echo: MagicMock) -> None:
        @argument_decorator((ScalarArguments, ComposeArguments))
        def mock_func(starwhale_argument: t.Tuple) -> None:
            ...

        ArgumentContext.get_current_context().echo_help()
        help_output = mock_echo.call_args[0][0]
        cases = [
            "tests.sdk.test_argument:ScalarArguments:",
            "--batch INTEGER",
            "--overwrite",
            "--learning_rate, --learning-rate FLOAT",
            "--half_precision_backend, --half-precision-backend TEXT",
            "--epoch INTEGER",
            "tests.sdk.test_argument:ComposeArguments:",
            "--debug DEBUGOPTION",
            "--lr_scheduler_kwargs, --lr-scheduler-kwargs DICT",
            "--evaluation_strategy, --evaluation-strategy [no|steps|epoch]",
            "--per_gpu_train_batch_size, --per-gpu-train-batch-size INTEGER",
            "--eval_delay, --eval-delay FLOAT",
            "--label_names, --label-names TEXT",
        ]
        for case in cases:
            assert case in help_output

    def test_argument_dict(self) -> None:
        @argument_decorator((ScalarArguments, ComposeArguments))
        def mock_f1(starwhale_argument: t.Tuple) -> None:
            ...

        @argument_decorator(ScalarArguments)
        def mock_f2(starwhale_argument: t.Tuple) -> None:
            ...

        @argument_decorator(ComposeArguments)
        def mock_f3(starwhale_argument: t.Tuple) -> None:
            ...

        info = ArgumentContext.get_current_context().asdict()
        assert len(info) == 3
        assert list(
            info[
                "tests.sdk.test_argument:ArgumentTestCase.test_argument_dict.<locals>.mock_f1"
            ].keys()
        ) == [
            "tests.sdk.test_argument:ScalarArguments",
            "tests.sdk.test_argument:ComposeArguments",
        ]
        batch = info[
            "tests.sdk.test_argument:ArgumentTestCase.test_argument_dict.<locals>.mock_f1"
        ]["tests.sdk.test_argument:ScalarArguments"]["batch"]
        assert batch == {
            "name": "batch",
            "opts": ["--batch"],
            "type": {
                "name": "integer",
                "param_type": "INT",
                "case_sensitive": False,
                "choices": None,
            },
            "required": False,
            "multiple": False,
            "default": 64,
            "help": "batch size",
            "is_flag": False,
            "hidden": False,
        }

        evaluation_strategy = info[
            "tests.sdk.test_argument:ArgumentTestCase.test_argument_dict.<locals>.mock_f1"
        ]["tests.sdk.test_argument:ComposeArguments"]["evaluation_strategy"]

        assert evaluation_strategy == {
            "default": "no",
            "help": "evaluation strategy",
            "hidden": False,
            "is_flag": False,
            "multiple": False,
            "name": "evaluation_strategy",
            "opts": ["--evaluation_strategy", "--evaluation-strategy"],
            "required": False,
            "type": {
                "case_sensitive": True,
                "choices": ["no", "steps", "epoch"],
                "name": "choice",
                "param_type": "CHOICE",
            },
        }

        debug = info[
            "tests.sdk.test_argument:ArgumentTestCase.test_argument_dict.<locals>.mock_f1"
        ]["tests.sdk.test_argument:ComposeArguments"]["debug"]

        assert debug == {
            "default": None,
            "help": "debug mode",
            "hidden": False,
            "is_flag": False,
            "multiple": True,
            "name": "debug",
            "opts": ["--debug"],
            "required": False,
            "type": {
                "name": "DebugOption",
                "param_type": "STRING",
                "case_sensitive": False,
                "choices": None,
            },
        }

        assert json.loads(json.dumps(info)) == info
