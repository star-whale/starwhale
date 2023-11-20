from enum import Enum
from typing import Any


class ServiceType(Enum):
    """Enumeration of service types."""

    TEXT_TO_TEXT = "text_to_text"
    TEXT_TO_IMAGE = "text_to_image"
    TEXT_TO_AUDIO = "text_to_audio"
    TEXT_TO_VIDEO = "text_to_video"
    QUESTION_ANSWERING = "question_answering"


Inputs = Any
Outputs = Any


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
