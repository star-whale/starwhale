import os

import torch
import gradio

from starwhale import Text, PipelineHandler
from starwhale.api.service import api

from .bleu import calculate_bleu
from .model import DecoderRNN, EncoderRNN
from .helper import EOS_token, SOS_token, MAX_LENGTH, sentence_to_tensor

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class NMTPipeline(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        self.hidden_size = 256
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.vocab = self._load_vocab()
        self.encoder = self._load_encoder_model(self.device)
        self.decoder = self._load_decoder_model(self.device)
        self.max_length = MAX_LENGTH

    @torch.no_grad()
    def ppl(self, data):
        input_tensor = sentence_to_tensor(
            self.vocab.vin, data["english"].content, self.device
        )
        input_length = input_tensor.size()[0]

        encoder_hidden = self.encoder.initHidden()
        encoder_outputs = torch.zeros(
            self.max_length, self.encoder.hidden_size, device=self.device
        )
        for ei in range(input_length):
            encoder_output, encoder_hidden = self.encoder(
                input_tensor[ei], encoder_hidden
            )
            encoder_outputs[ei] += encoder_output[0, 0]

        decoder_input = torch.tensor([[SOS_token]], device=self.device)
        decoder_hidden = encoder_hidden
        decoded_words = []
        decoder_attentions = torch.zeros(self.max_length, self.max_length)

        for di in range(self.max_length):
            decoder_output, decoder_hidden, decoder_attention = self.decoder(
                decoder_input, decoder_hidden, encoder_outputs
            )
            decoder_attentions[di] = decoder_attention.data
            _, topi = decoder_output.data.topk(1)
            if topi.item() == EOS_token:
                break
            else:
                decoded_words.append(self.vocab.vout.index2word[topi.item()])
            decoder_input = topi.squeeze().detach()

        return " ".join(decoded_words)

    def cmp(self, _data_loader):
        result, label = [], []
        for _data in _data_loader:
            result.append(_data["output"])
            label.append(_data["input"]["french"].content)

        bleu = calculate_bleu(result, [label])
        print(f"bleu: {bleu}")
        report = {"bleu_score": bleu}
        self.evaluation_store.log_summary_metrics(report)

    def _load_vocab(self):
        # hack for torch load
        from .helper import Lang, Vocab  # noqa

        return torch.load(_ROOT_DIR + "/models/vocab_eng-fra.bin")

    def _load_encoder_model(self, device):
        model = EncoderRNN(self.vocab.vin.n_words, self.hidden_size, device).to(device)

        param = torch.load(_ROOT_DIR + "/models/encoder.pth", map_location=device)
        model.load_state_dict(param)
        return model

    def _load_decoder_model(self, device):
        model = DecoderRNN(self.vocab.vout.n_words, self.hidden_size, device).to(device)
        param = torch.load(_ROOT_DIR + "/models/decoder.pth", map_location=device)
        model.load_state_dict(param)
        return model

    @api(
        gradio.Text(label="en"),
        gradio.Text(label="fr"),
        examples=["i m not afraid to die .", "i study mathematics ."],
    )
    def online_eval(self, content: str):
        return self.ppl({"english": Text(content)})
