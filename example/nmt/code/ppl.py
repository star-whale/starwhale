
import os
from regex import W

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

    def __init__(self) -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.vocab = self._load_vocab()
        self.encoder = self._load_encoder_model(self.device)
        self.decoder = self._load_decoder_model(self.device)

    @torch.no_grad()
    def ppl(self, data, batch_size, **kw):
        print(f"-----> ppl: {len(data)}, {batch_size}")
        src_sentences = data.decode().split('\n')
        print("ppl-src sentexces: %s" % len(src_sentences))
        return evaluate_batch(self.device, self.vocab.input_lang, self.vocab.output_lang, src_sentences, self.encoder, self.decoder)

    def handle_label(self, label, batch_size, **kw):
        labels = label.decode().split('\n')
        print("src lebels: %s" % len(labels))
        return labels


    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            _label.extend(_data[self._label_field])
            (result) = _data[self._ppl_data_field]
            _result.extend(result)
        
        # print("cmp-result:%s" % len(_result))
        # for r in _result:
        #     print(r)
        
        # print("cmp-label:%s" % len(_label))
        # for l in _label:
        #     print(l)

        bleu = BLEU(_result, [_label])

        return {'summary': {'bleu_score': bleu}}


    def _load_vocab(self):
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
