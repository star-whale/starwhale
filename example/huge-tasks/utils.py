import random
import string


def random_text() -> str:
    return "".join([random.choice(string.ascii_letters) for i in range(15)])
