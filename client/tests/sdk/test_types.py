import pytest

from starwhale.api._impl.service.types import all_components_are_gradio


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
