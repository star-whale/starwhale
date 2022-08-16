import os

import torch
from regex import W

from starwhale.api.model import PipelineHandler

try:
    from .eval import evaluate_batch
    from .model import EncoderRNN, AttnDecoderRNN
    from .vocab import Lang, Vocab
    from .calculatebleu import BLEU
except ImportError:
    from eval import evaluate_batch
    from model import EncoderRNN, AttnDecoderRNN
    from vocab import Lang, Vocab
    from calculatebleu import BLEU

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class NMTPipeline(PipelineHandler):
    def __init__(self) -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.vocab = self._load_vocab()
        self.encoder = self._load_encoder_model(self.device)
        self.decoder = self._load_decoder_model(self.device)

    @torch.no_grad()
    def ppl(self, data, **kw):
        print(f"-----> ppl: {len(data)}")
        src_sentences = data.decode().split("\n")
        print("ppl-src sentexces: %s" % len(src_sentences))
        return evaluate_batch(
            self.device,
            self.vocab.input_lang,
            self.vocab.output_lang,
            src_sentences,
            self.encoder,
            self.decoder,
        )

    def handle_label(self, label, **kw):
        labels = label.decode().split("\n")
        print("src labels: %s" % len(labels))
        return labels

    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            _label.extend(_data[self._label_field])
            (result) = _data[self._ppl_data_field]
            _result.extend(result)

        bleu = BLEU(_result, [_label])

        return {"summary": {"bleu_score": bleu}}

    def _load_vocab(self):
        return torch.load(_ROOT_DIR + "/models/vocab_eng-fra.bin")

    def _load_encoder_model(self, device):

        hidden_size = 256
        model = EncoderRNN(self.vocab.input_lang.n_words, hidden_size, device).to(
            device
        )

        param = torch.load(_ROOT_DIR + "/models/encoder.pth", device)
        model.load_state_dict(param)

        return model

    def _load_decoder_model(self, device):

        hidden_size = 256
        model = AttnDecoderRNN(self.vocab.output_lang.n_words, hidden_size, device).to(
            device
        )

        param = torch.load(_ROOT_DIR + "/models/decoder.pth", device)
        model.load_state_dict(param)

        return model
