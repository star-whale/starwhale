def simple_text_iter():
    for idx in range(0, 5):
        yield {
            "txt": f"data-{idx}",
            "label": f"label-{idx}",
        }
