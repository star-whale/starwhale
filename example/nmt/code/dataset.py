from time import process_time_ns
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
    pairs = [[normalizeString(s) for s in l.split('\t') if not filterComment(s) and s] for l in lines]

    return pairs


class DataSetProcessExecutor(BuildExecutor):
    # default param self._batch
    def iter_data_slice(self, path: str):
        pairs = prepareData(path)
        index = 0
        lines = len(pairs)
        while True:
            last_index = index
            index = index + self._batch
            index = min(index, lines - 1 )
            print('data:%s, %s' % (last_index, index))
            data_batch = [src for src, tgt in pairs[last_index:index]]
            join = "\n".join(data_batch)
            
            print("res-data:%s" % join)
            yield join.encode()
            if index >= lines - 1:
                break

    def iter_label_slice(self, path: str):
        pairs = prepareData(path)
        index = 0
        lines = len(pairs)
        while True:
            last_index = index
            index = index + self._batch
            index = min(index, lines - 1)
            
            print('label:%s, %s' % (last_index, index))
            data_batch = [tgt for src, tgt in pairs[last_index:index]]
            join = "\n".join(data_batch)
            print("res-label:%s" % join)
            yield join.encode()
            if index >= lines - 1:
                break
        
