from typing import List

from starwhale.api.service import api, LLMChat
from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentValueSpecFloat,
)


@api(
    inference_type=LLMChat(
        top_p=ComponentValueSpecFloat(defaultVal=0.9, max=1.0, min=0.1, step=0.1),
        temperature=ComponentValueSpecFloat(defaultVal=0.5, step=0.01),
        max_new_tokens=ComponentValueSpecInt(defaultVal=100, max=1000, min=10),
    )
)
def fake_chat_bot(
    user_input: str,
    history: List[dict],
    temperature: float,
    top_k: int,
    top_p: float,
    max_new_tokens: int,
) -> List[dict]:
    result = f"hello from chat bot with {user_input}, and temperature {temperature}, and top_k {top_k}, and top_p {top_p}, and max_new_tokens {max_new_tokens}"
    history.extend(
        [{"content": user_input, "role": "user"}, {"content": result, "role": "bot"}]
    )
    return history
