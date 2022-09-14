import os
import math
import time
import random
import typing as t

import torch
import torch.nn as nn
from torch import optim

from .model import DecoderRNN, EncoderRNN
from .helper import (
    Lang,
    Vocab,
    EOS_token,
    SOS_token,
    MAX_LENGTH,
    normalize_str,
    sentence_to_tensor,
)

teacher_forcing_ratio = 0.5

_ROOT_DIR = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))
_ENCODER_MODEL_PATH = os.path.join(_ROOT_DIR, "models/encoder.pth")
_DECODER_MODEL_PATH = os.path.join(_ROOT_DIR, "models/decoder.pth")


def pair_to_tensor(input_lang, output_lang, pair, device):
    input_tensor = sentence_to_tensor(input_lang, pair[0], device)
    target_tensor = sentence_to_tensor(output_lang, pair[1], device)
    return (input_tensor, target_tensor)


def as_minutes(s):
    m = math.floor(s / 60)
    s -= m * 60
    return f"{m}m {s}s"


def time_since(since, percent):
    now = time.time()
    s = now - since
    es = s / (percent)
    rs = es - s
    return "%s (- %s)" % (as_minutes(s), as_minutes(rs))


def filter_comment(s):
    return s.startswith("CC-BY")


def prepare_data(path):
    print("preparing data...")
    lines = open(path, encoding="utf-8").read().strip().split("\n")

    pairs = []
    for line in lines:
        pairs.append(
            [normalize_str(s) for s in line.split("\t") if not filter_comment(s) and s]
        )
    return pairs


def build_vocab(
    src_lang: str, target_lang: str, vocab_path: str, train_data_path: str
) -> t.Tuple[t.Any, t.Any]:
    in_lang = Lang(src_lang)
    out_lang = Lang(target_lang)

    with open(train_data_path, "r") as f:
        for line in f.readlines():
            line = line.strip()
            if not line:
                continue

            _tp = [normalize_str(s) for s in line.split("\t") if not filter_comment(s)]
            in_lang.add_sentence(_tp[0])
            out_lang.add_sentence(_tp[1])

    vocab = Vocab(in_lang, out_lang)
    torch.save(vocab, vocab_path)
    print(
        f"generated vocabulary, source {vocab.vin.n_words} words, target {vocab.vout.n_words} words"
    )
    return in_lang, out_lang


def train_once(
    device,
    input_tensor,
    target_tensor,
    encoder,
    decoder,
    encoder_optimizer,
    decoder_optimizer,
    criterion,
    max_length=MAX_LENGTH,
):
    encoder_hidden = encoder.initHidden()

    encoder_optimizer.zero_grad()
    decoder_optimizer.zero_grad()

    input_length = input_tensor.size(0)
    target_length = target_tensor.size(0)

    encoder_outputs = torch.zeros(max_length, encoder.hidden_size, device=device)

    loss = 0

    for ei in range(input_length):
        encoder_output, encoder_hidden = encoder(input_tensor[ei], encoder_hidden)
        encoder_outputs[ei] = encoder_output[0, 0]

    decoder_input = torch.tensor([[SOS_token]], device=device)

    decoder_hidden = encoder_hidden

    use_teacher_forcing = True if random.random() < teacher_forcing_ratio else False

    if use_teacher_forcing:
        # Teacher forcing: Feed the target as the next input
        for di in range(target_length):
            decoder_output, decoder_hidden, decoder_attention = decoder(
                decoder_input, decoder_hidden, encoder_outputs
            )
            loss += criterion(decoder_output, target_tensor[di])
            decoder_input = target_tensor[di]  # Teacher forcing

    else:
        # Without teacher forcing: use its own predictions as the next input
        for di in range(target_length):
            decoder_output, decoder_hidden, decoder_attention = decoder(
                decoder_input, decoder_hidden, encoder_outputs
            )
            _, topi = decoder_output.topk(1)
            decoder_input = topi.squeeze().detach()  # detach from history as input

            loss += criterion(decoder_output, target_tensor[di])
            if decoder_input.item() == EOS_token:
                break

    loss.backward()

    encoder_optimizer.step()
    decoder_optimizer.step()

    return loss.item() / target_length


def train(
    input_lang,
    output_lang,
    train_data_path,
    n_iters=10000,
    print_every=1000,
    plot_every=100,
    learning_rate=0.01,
):
    start = time.time()
    hidden_size = 256
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    encoder = EncoderRNN(input_lang.n_words, hidden_size, device).to(device)
    decoder = DecoderRNN(output_lang.n_words, hidden_size, device, dropout_p=0.1).to(
        device
    )
    pairs = prepare_data(train_data_path)

    plot_losses = []
    print_loss_total = 0
    plot_loss_total = 0

    encoder_optimizer = optim.SGD(encoder.parameters(), lr=learning_rate)
    decoder_optimizer = optim.SGD(decoder.parameters(), lr=learning_rate)
    training_pairs = [
        pair_to_tensor(input_lang, output_lang, random.choice(pairs), device)
        for i in range(n_iters)
    ]
    criterion = nn.NLLLoss()

    for iter in range(1, n_iters + 1):
        training_pair = training_pairs[iter - 1]
        input_tensor = training_pair[0]
        target_tensor = training_pair[1]

        loss = train_once(
            device,
            input_tensor,
            target_tensor,
            encoder,
            decoder,
            encoder_optimizer,
            decoder_optimizer,
            criterion,
        )
        print_loss_total += loss
        plot_loss_total += loss

        if iter % print_every == 0:
            print_loss_avg = print_loss_total / print_every
            print_loss_total = 0
            print(
                "%s (%d %d%%) %.4f"
                % (
                    time_since(start, iter / n_iters),
                    iter,
                    iter / n_iters * 100,
                    print_loss_avg,
                )
            )

        if iter % plot_every == 0:
            plot_loss_avg = plot_loss_total / plot_every
            plot_losses.append(plot_loss_avg)
            plot_loss_total = 0

    print("Saving model to {}".format(_ENCODER_MODEL_PATH))
    torch.save(encoder.state_dict(), _ENCODER_MODEL_PATH)
    print("Saving model to {}".format(_DECODER_MODEL_PATH))
    torch.save(decoder.state_dict(), _DECODER_MODEL_PATH)
