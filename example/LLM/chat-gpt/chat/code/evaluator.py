import os
import openai
openai.api_key = os.getenv("OPENAI_API_KEY")

from starwhale import evaluation


@evaluation.predict
def ppl(data: dict, **kw):
    text = data["text"]
    chat_result = openai.ChatCompletion.create(model="gpt-3.5-turbo", messages=[
        {
            "role": "user",
            "content": text
        }
    ])
    response = chat_result.choices[0].message.content
    print(f"U: {text}\n GPT: {response} \n")
    return response
