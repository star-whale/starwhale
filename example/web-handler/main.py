from typing import List

from starwhale.api.service import api, LLMChat


@api(inference_type=LLMChat())
def fake_chat_bot(
    user_input: str,
    history: List[LLMChat.Message],
    temperature: float = 0.5,
    top_p: float = 0.9,
    top_k: int = 1,
    max_new_tokens: int = 100,
) -> List[dict]:
    result = f"hello from chat bot with {user_input}, and temperature {temperature}, and top_k {top_k}, and top_p {top_p}, and max_new_tokens {max_new_tokens}"
    history.extend(
        [LLMChat.Message(content=user_input), LLMChat.Message(content=result, bot=True)]
    )
    return history
