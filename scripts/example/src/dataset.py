from starwhale import Text

try:
    from .util import random_image
except ImportError:
    from util import random_image


def simple_text_iter():
    for idx in range(0, 5):
        yield {
            "img": random_image(),
            "txt": Text(f"txt-{idx}"),
            "label": f"label-{idx}",
            "placeholder": 1,
        }
