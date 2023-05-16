from starwhale import Text, Image


def simple_text_iter():
    for idx in range(0, 5):
        yield {
            "img": Image(b"123"),
            "txt": Text(f"txt-{idx}"),
            "label": f"label-{idx}",
            "placeholder": 1,
        }
