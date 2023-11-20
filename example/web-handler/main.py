from typing import List

from starwhale.api.service import api, QuestionAnswering


@api(inference_type=QuestionAnswering(args={"user_input", "history", "temperature"}))
def fake_chat_bot(
    user_input: str, history: List[dict], temperature: float
) -> List[dict]:
    result = f"hello from chat bot with {user_input}, and temperature {temperature}"
    history.extend(
        [{"content": user_input, "role": "user"}, {"content": result, "role": "bot"}]
    )
    return history
