import os

import numpy as np
import torch
from torchtext.data.utils import get_tokenizer, ngrams_iterator

from starwhale import PipelineHandler, multi_classification

from .model import TextClassificationModel

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_LABEL_NAMES = ["World", "Sports", "Business", "Sci/Tech"]
_NUM_CLASSES = len(_LABEL_NAMES)


class TextClassificationHandler(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.tokenizer = get_tokenizer("basic_english")
        self.model = self._load_model(self.device)
        self.vocab = self._load_vocab()

    @torch.no_grad()
    def ppl(self, data: dict, **kw):
        ngrams = list(ngrams_iterator(self.tokenizer(data["text"]), 2))
        tensor = torch.tensor(self.vocab(ngrams)).to(self.device)
        output = self.model(tensor, torch.tensor([0]).to(self.device))
        pred_value = output.argmax(1).item()
        pr_matrix = np.exp(output.tolist()).tolist()
        return pred_value, pr_matrix[0]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, _NUM_CLASSES)],
    )
    def cmp(self, ppl_result):
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["ds_data"]["label"])
            result.append(_data["result"][0])
            pr.append(_data["result"][1])

        return label, result, pr

    def _load_model(self, device):
        model_path = _ROOT_DIR + "/models/model.i"
        model = TextClassificationModel(1308713, 32, _NUM_CLASSES).to(device)
        model.load_state_dict(torch.load(model_path, map_location=device))
        model.eval()
        return model

    def _load_vocab(self):
        vocab_path = _ROOT_DIR + "/models/vocab.i"
        vocab = torch.load(vocab_path)
        return vocab
