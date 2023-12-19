from typing import List, Optional

import pytest
from pydantic import BaseModel

from starwhale.api._impl.service.types import all_components_are_gradio
from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentSpecValueType,
)
from starwhale.api._impl.service.types.types import generate_type_definition
from starwhale.api._impl.service.types.text_to_img import TextToImage


@pytest.mark.skip("enable this test when starwhale support pydantic 2.0+")
def test_all_components_are_gradio():
    try:
        import gradio
    except ImportError:
        gradio = None

    assert all_components_are_gradio(None, None) is False

    assert all_components_are_gradio(gradio.inputs.Textbox(), None) is True
    assert all_components_are_gradio([gradio.inputs.Textbox()], None) is True
    assert all_components_are_gradio(None, [gradio.outputs.Label()]) is True
    assert all_components_are_gradio(None, gradio.outputs.Label()) is True
    assert (
        all_components_are_gradio(gradio.inputs.Textbox(), gradio.outputs.Label())
        is True
    )
    assert (
        all_components_are_gradio([gradio.inputs.Textbox()], [gradio.outputs.Label()])
        is True
    )


def test_text_to_image():
    t = TextToImage()

    assert t.name == "text_to_image"
    assert t.args == {}
    assert t.arg_types == {
        "prompt": ComponentSpecValueType.string,
        "negative_prompt": ComponentSpecValueType.string,
        "sampling_steps": ComponentSpecValueType.int,
        "width": ComponentSpecValueType.int,
        "height": ComponentSpecValueType.int,
        "seed": ComponentSpecValueType.int,
        "batch_size": ComponentSpecValueType.int,
        "batch_count": ComponentSpecValueType.int,
        "guidance_scale": ComponentSpecValueType.float,
    }

    t = TextToImage(
        sampling_steps=ComponentValueSpecInt(default_val=1),
    )

    assert t.args == {
        "sampling_steps": ComponentValueSpecInt(default_val=1),
    }


def test_generate_type_definition():
    class MyType(BaseModel):
        a: int
        b: str
        c: float
        d: bool
        e: Optional[int] = None
        f: Optional[str] = None
        g: Optional[float] = None
        h: Optional[bool] = None
        i: List[int]
        j: List[str]
        k: Optional[List[int]] = None

    types = generate_type_definition(MyType)
    assert types == {
        "a": ComponentSpecValueType.int,
        "b": ComponentSpecValueType.string,
        "c": ComponentSpecValueType.float,
        "d": ComponentSpecValueType.bool,
        "e": ComponentSpecValueType.int,
        "f": ComponentSpecValueType.string,
        "g": ComponentSpecValueType.float,
        "h": ComponentSpecValueType.bool,
        "i": ComponentSpecValueType.list,
        "j": ComponentSpecValueType.list,
        "k": ComponentSpecValueType.list,
    }
