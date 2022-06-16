from starwhale.api.dataset import BuildExecutor

from io import open

try:
    from .helper import filterComment, normalizeString
except ImportError:
    from helper import filterComment, normalizeString

def prepareData(path):
    print('preapring data...')
    # Read the file and split into lines
    lines = open(path, encoding='utf-8').\
        read().strip().split('\n')

    # Split every line into pairs and normalize
    pairs = [[normalizeString(s) for s in l.split('\t') if not filterComment(s)] for l in lines]

    return pairs


class DataSetProcessExecutor(BuildExecutor):

    def iter_data_slice(self, path: str):
        pairs = prepareData(path)
        for pair in pairs:
            yield pair[0].append('\n')

    def iter_label_slice(self, path: str):
        pairs = prepareData(path)
        for pair in pairs:
            yield pair[1].append('\n')
