import os

from Bard import Chatbot

from starwhale import evaluation

# This evaluation shall be run in USA or UK, or errors would be raised
# https://github.com/acheong08/Bard
bard_key = os.getenv("BARD_TOKEN")
chatbot = Chatbot(bard_key)


@evaluation.predict
def ppl(data: dict, **kw):
    text_ = data["text"]
    response = chatbot.ask(text_).get("content")
    print(f"U: {text_}\n Bard: {response} \n")
    return response
