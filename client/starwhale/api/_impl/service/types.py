from enum import Enum


class ServiceType(Enum):
    """Enumeration of service types."""

    TEXT_TO_TEXT = "text_to_text"
    TEXT_TO_IMAGE = "text_to_image"
    TEXT_TO_AUDIO = "text_to_audio"
    TEXT_TO_VIDEO = "text_to_video"
    QUESTION_ANSWERING = "question_answering"
