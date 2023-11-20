from typing import List

from starwhale.api.service import api, LLMChat


@api(inference_type=LLMChat())
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
