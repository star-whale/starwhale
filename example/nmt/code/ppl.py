
import os

import torch
from starwhale.api.model import PipelineHandler

try:
    from .vocab import Vocab, Lang
    from .calculatebleu import BLEU
    from .eval import evaluate_batch
    from .model import EncoderRNN, AttnDecoderRNN
except ImportError:
    from vocab import Vocab, Lang
    from calculatebleu import BLEU
    from eval import evaluate_batch
    from model import EncoderRNN, AttnDecoderRNN

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class NMTPipeline(PipelineHandler):

    def __init__(self, device="cuda") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)
        self.vocab = self._load_vocab()
        self.encoder = self._load_encoder_model(self.device)
        self.decoder = self._load_decoder_model(self.device)

    @torch.no_grad()
    def ppl(self, data, batch_size, **kw):
        src_sentences = data.split('\n')
        return evaluate_batch(self.device, self.vocab.input_lang, self.vocab.output_lang, src_sentences, self.encoder, self.decoder), None

    def handle_label(self, label, batch_size, **kw):
        tgt_sentences = label.split('\n')
        return tgt_sentences


    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            _label.extend(_data["label"])
            _result.extend(_data["result"])
        bleu = BLEU(_result, _label)

        return [{'bleu_score': bleu}]


    def _load_vocab(self):
        from .vocab import Vocab, Lang
        return torch.load(_ROOT_DIR + '/models/vocab_eng-fra.bin')

    def _load_encoder_model(self, device):

        hidden_size = 256
        model = EncoderRNN(self.vocab.input_lang.n_words, hidden_size, device).to(device)

        param = torch.load(_ROOT_DIR + "/models/encoder.pth", device)
        model.load_state_dict(param)
        
        return model
    
    def _load_decoder_model(self, device):

        hidden_size = 256
        model = AttnDecoderRNN(self.vocab.output_lang.n_words, hidden_size, device).to(device)

        param = torch.load(_ROOT_DIR + "/models/decoder.pth", device)
        model.load_state_dict(param)
        
        return model
