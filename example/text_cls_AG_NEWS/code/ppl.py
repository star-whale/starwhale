import os

import torch
from torchtext.data.utils import get_tokenizer

from starwhale.api.job import Context
from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

try:
    from . import model, predict
except ImportError:
    import model
    import predict


_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class TextClassificationHandler(PipelineHandler):
    def __init__(self, context: Context) -> None:
        super().__init__(context=context)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    @torch.no_grad()
    def ppl(self, data, **kw):
        _model, vocab, tokenizer = self._load_model(self.device)
        texts = data.decode().split("#@#@#@#")
        return list(
            map(lambda text: predict.predict(text, _model, vocab, tokenizer, 2), texts)
        )

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=False,
        all_labels=[i for i in range(1, 5)],
    )
    def cmp(self, ppl_result):
        result, label = [], []
        for _data in ppl_result:
            label.append(_data["annotations"]["label"])
            (result) = _data["result"]
            result.extend([int(r) for r in result])
        return label, result

    def _load_model(self, device):
        model_path = _ROOT_DIR + "/models/model.i"
        _model = model.TextClassificationModel(1308713, 32, 4).to(device)
        _model.load_state_dict(torch.load(model_path))
        _model.eval()
        vocab_path = _ROOT_DIR + "/models/vocab.i"
        dictionary = torch.load(vocab_path)
        tokenizer = get_tokenizer("basic_english")
        return _model, dictionary, tokenizer
