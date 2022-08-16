import os

import torch
from torchtext.data.utils import get_tokenizer

from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

try:
    from . import predict
except ImportError:
    import predict

try:
    from . import model
except ImportError:
    import model


_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class TextClassificationHandler(PipelineHandler):
    def __init__(self, device="cpu") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)

    @torch.no_grad()
    def ppl(self, data, **kw):
        _model, vocab, tokenizer = self._load_model(self.device)
        texts = data.decode().split("#@#@#@#")
        return list(
            map(lambda text: predict.predict(text, _model, vocab, tokenizer, 2), texts)
        )

    def handle_label(self, label, **kw):
        labels = label.decode().split("#@#@#@#")
        return [int(label) for label in labels]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=False,
        all_labels=[i for i in range(1, 5)],
    )
    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            print(_data)
            _label.extend([int(l) for l in _data[self._label_field]])
            (result) = _data[self._ppl_data_field]
            _result.extend([int(r) for r in result])
        return _label, _result

    def _load_model(self, device):
        model_path = _ROOT_DIR + "/models/model.i"
        _model = model.TextClassificationModel(1308713, 32, 4).to(device)
        _model.load_state_dict(torch.load(model_path))
        _model.eval()
        vocab_path = _ROOT_DIR + "/models/vocab.i"
        dictionary = torch.load(vocab_path)
        tokenizer = get_tokenizer("basic_english")
        return _model, dictionary, tokenizer


def load_test_env_cmp(fuse=True):

    os.environ["SW_TASK_STATUS_DIR"] = "status/cmp"
    os.environ["SW_TASK_LOG_DIR"] = "log/cmp"
    os.environ["SW_TASK_RESULT_DIR"] = "result/cmp"

    # fname = "swds_fuse_simple.json" if fuse else "swds_s3_simple.json"
    os.environ["SW_TASK_INPUT_CONFIG"] = "input.json"


def load_test_env_ppl(fuse=True):
    os.environ["SW_TASK_STATUS_DIR"] = "status"
    os.environ["SW_TASK_LOG_DIR"] = "log"
    os.environ["SW_TASK_RESULT_DIR"] = "result"

    # fname = "swds_fuse_simple.json" if fuse else "swds_s3_simple.json"
    os.environ[
        "SW_TASK_INPUT_CONFIG"
    ] = "/home/anda/.cache/starwhale/self/dataset/ag_news/ga/gaygmnztgq2wgmrsmuydgy3enayhmzy.swds/local_fuse.json"


if __name__ == "__main__":
    # load_test_env_ppl(fuse=True)
    load_test_env_cmp(fuse=True)
    text_cls = TextClassificationHandler()
    text_cls._starwhale_internal_run_cmp()
    # text_cls._starwhale_internal_run_ppl()
