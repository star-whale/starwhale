from starwhale.api.service import api, ServiceType


@api(ServiceType.QUESTION_ANSWERING)
def fake_chat_bot(content: str) -> str:
    return f"hello from chat bot with {content}"
