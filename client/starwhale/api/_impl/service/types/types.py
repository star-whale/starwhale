from __future__ import annotations

import abc
import inspect
from typing import Any, Dict, List, Union, Callable

from typing_extensions import Protocol

from starwhale.utils import console
from starwhale.base.client.models.models import (
    ComponentSpec,
    ComponentValueSpecInt,
    ComponentSpecValueType,
    ComponentValueSpecBool,
    ComponentValueSpecFloat,
    ComponentValueSpecString,
)

Inputs = Any
Outputs = Any


class ComponentValueSpec(Protocol):
    default_val: Any


ArgSpec = Union[
    ComponentValueSpec,
    None,
]


class ServiceType(abc.ABC):
    """Protocol for service types."""

    @property
    @abc.abstractmethod
    def arg_types(self) -> Dict[str, ComponentSpecValueType]:
        ...

    @property
    @abc.abstractmethod
    def args(self) -> Dict[str, ArgSpec]:
        ...

    @property
    @abc.abstractmethod
    def name(self) -> str:
        ...

    def _validate_fn_with_arg_types(self, func: Callable) -> None:
        """Validate the function with the argument types."""
        sig = inspect.signature(func)
        params = sig.parameters

        # check the type of each argument
        for name, param in params.items():
            expected_type = self.arg_types[name]
            arg_type = param.annotation
            if arg_type is inspect.Parameter.empty:
                console.warn(f"Argument type {name} is not specified.")
                continue
            if arg_type is not expected_type:
                raise ValueError(
                    f"Argument type {name} should be {expected_type}, not {arg_type}."
                )

    def validate(self, value: Callable) -> None:
        """
        Validate the service type
        The function should raise a ValueError if the function is not valid.
        :param value: the function to validate
        """
        self._validate_fn_with_arg_types(value)

    @abc.abstractmethod
    def router_fn(self, func: Callable) -> Callable:
        ...

    def components_spec(self) -> List[ComponentSpec]:
        ret = []
        for name, arg in self.args.items():
            item = ComponentSpec(
                name=name, component_spec_value_type=self.arg_types[name]
            )
            if isinstance(arg, ComponentValueSpecInt):
                item.component_value_spec_int = arg
            elif isinstance(arg, ComponentValueSpecFloat):
                item.component_value_spec_float = arg
            elif isinstance(arg, ComponentValueSpecString):
                item.component_value_spec_string = arg
            elif isinstance(arg, ComponentValueSpecBool):
                item.component_value_spec_bool = arg
            ret.append(item)

        return ret

    def update_arg(self, name: str, default_value: Any) -> None:
        """
        Update the argument with the default value.
        Only mark the argument as required if the default value is None.
        :param name: the name of the argument
        :param default_value: the default value of the argument, if None, only add the argument to the api spec
            without default value
        """

        if name not in self.arg_types:
            raise ValueError(f"Argument {name} does not support.")

        if name in self.args:
            # if the original default value is None, update it
            arg_val_spec = self.args[name]
            if arg_val_spec is not None and arg_val_spec.default_val is None:
                arg_val_spec.default_val = default_value
            return

        if default_value is None:
            self.args[name] = None
            return

        arg_type = self.arg_types[name]
        if arg_type == ComponentSpecValueType.int:
            self.args[name] = ComponentValueSpecInt(default_val=default_value)
        elif arg_type == ComponentSpecValueType.float:
            self.args[name] = ComponentValueSpecFloat(default_val=default_value)
        elif arg_type == ComponentSpecValueType.string:
            self.args[name] = ComponentValueSpecString(default_val=default_value)
        elif arg_type == ComponentSpecValueType.bool:
            self.args[name] = ComponentValueSpecBool(default_val=default_value)

    def update_from_func(self, func: Callable) -> None:
        """Update the service type components spec from the function."""
        sig = inspect.signature(func)
        params = sig.parameters

        for name, param in params.items():
            if name == "self":
                continue
            df = param.default if param.default is not inspect.Parameter.empty else None
            self.update_arg(name, df)


def all_components_are_gradio(
    inputs: Inputs, outputs: Outputs
) -> bool:  # pragma: no cover
    """Check if all components are Gradio components."""
    if inputs is None and outputs is None:
        return False

    if not isinstance(inputs, list):
        inputs = inputs is not None and [inputs] or []
    if not isinstance(outputs, list):
        outputs = outputs is not None and [outputs] or []

    try:
        import gradio
    except ImportError:
        gradio = None

    return all(
        [
            gradio is not None,
            all([isinstance(inp, gradio.components.Component) for inp in inputs]),
            all([isinstance(out, gradio.components.Component) for out in outputs]),
        ]
    )
