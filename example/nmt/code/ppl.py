import os

import torch

from starwhale.api.job import Context
from starwhale.api.model import PipelineHandler

try:
    from .eval import evaluate_batch
    from .model import EncoderRNN, AttnDecoderRNN
    from .calculatebleu import BLEU
except ImportError:
    from eval import evaluate_batch
    from model import EncoderRNN, AttnDecoderRNN
    from calculatebleu import BLEU

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class NMTPipeline(PipelineHandler):
    def __init__(self, context: Context) -> None:
        super().__init__(context=context)

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

    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            _label.extend(_data["annotations"]["label"])
            (result) = _data["result"]
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
