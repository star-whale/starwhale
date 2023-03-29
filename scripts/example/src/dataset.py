from starwhale import Text


def simple_text_iter():
    for idx in range(0, 100):
        yield {
            "txt": Text(f"data-{idx}", encoding="utf-8"),
            "label": f"label-{idx}",
        }
