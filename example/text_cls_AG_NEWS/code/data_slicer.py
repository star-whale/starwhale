from starwhale.api.dataset import BuildExecutor

from . import ag_news


def yield_data(batch, path, label=False):
    data = ag_news.load_ag_data(path)
    idx = 0
    data_size = len(data)
    while True:
        last_idx = idx
        idx = idx + batch
        if idx > data_size:
            break
        data_batch = [lbl if label else txt for lbl, txt in
                      data[last_idx:idx]]
        join = "#@#@#@#".join(data_batch)
        yield join.encode()

class AGNEWSSlicer(BuildExecutor):

    def iter_data_slice(self, path: str):
        yield from yield_data(self._batch, path)

    def iter_label_slice(self, path: str):
        yield from yield_data(self._batch, path, True)
