import os

import torch
import gradio
from torchtext.data.utils import get_tokenizer, ngrams_iterator

from starwhale import Text, PipelineHandler, multi_classification
from starwhale.api.service import api

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
        print(f"index: {kw['external']['index']}")
        ngrams = list(ngrams_iterator(self.tokenizer(data["text"]), 2))
        tensor = torch.tensor(self.vocab(ngrams)).to(self.device)
        output = self.model(tensor, torch.tensor([0]).to(self.device))
        pred_value = output.argmax(1).item()
        pr_matrix = torch.nn.Softmax(dim=1)(output)
        return pred_value, pr_matrix[0].tolist()

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

    @api(
        gradio.Text(label="input"),
        gradio.Label(),
        examples=[
            "Fears for T N pension after talks Unions representing workers at Turner Newall say they are 'disappointed' after talks with stricken parent firm Federal Mogul.",
            'E-mail scam targets police chief Wiltshire Police warns about "phishing" after its fraud squad chief was targeted.',
        ],
    )
    def online_eval(self, content: str):
        _, prob = self.ppl(Text(content))
        return {_LABEL_NAMES[i]: p for i, p in enumerate(prob)}
