import typing as t

from starwhale import Text, BuildExecutor


class SimpleTextDatasetBuildExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        for idx in range(0, 100):
            data = Text(f"data-{idx}", encoding="utf-8")
            annotations = {"label": f"label-{idx}"}
            yield data, annotations
