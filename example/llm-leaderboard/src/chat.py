from __future__ import annotations

from starwhale import handler


@handler(expose=17860)
def chat_web():
    ...


@handler()
def chat_terminal():
    ...
