import uuid
import hashlib
import base64
import random
import string

from loguru import logger


def gen_uniq_version(feature: str = ""):
    # version = ${timestamp:8} + ${feature:8} + ${randstr:4}
    timestamp = "".join(str(uuid.uuid1()).split("-")[0])
    feature = hashlib.sha256((feature or random_str()).encode()).hexdigest()[:7]
    randstr = random_str(cnt=4)
    bstr = base64.b32encode(f"{timestamp}{feature}{randstr}".encode()).decode("ascii")
    # TODO: add test for uniq and number
    return bstr.lower().strip("=")


def random_str(cnt: int = 8) -> str:
    return "".join(random.sample(string.ascii_lowercase + string.digits, cnt))
