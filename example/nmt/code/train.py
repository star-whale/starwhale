import os
import random
import time
import torch
from torch import optim
import torch.nn as nn
try:
    from .config import MAX_LENGTH, EOS_token, SOS_token
    from .dataset import prepareData
    from .eval import evaluateRandomly
    from .helper import tensorsFromPair, timeSince
    from .model import AttnDecoderRNN, EncoderRNN
    from .vocab import Vocab, Lang
except ImportError:
    from config import MAX_LENGTH, EOS_token, SOS_token
    from dataset import prepareData
    from eval import evaluateRandomly
    from helper import tensorsFromPair, timeSince
    from model import AttnDecoderRNN, EncoderRNN
    from vocab import Vocab, Lang

teacher_forcing_ratio = 0.5

_ROOT_DIR = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))
_ENCODER_MODEL_PATH = os.path.join(_ROOT_DIR, "models/encoder.pth")
_DECODER_MODEL_PATH = os.path.join(_ROOT_DIR, "models/decoder.pth")
_VOCAB_PATH = os.path.join(_ROOT_DIR, "models/vocab_eng-fra.bin")
_DATA_PATH = os.path.join(_ROOT_DIR, "data/train_eng-fra.txt")

def train(device, input_tensor, target_tensor, encoder, decoder, encoder_optimizer, decoder_optimizer, criterion, max_length=MAX_LENGTH):
    encoder_hidden = encoder.initHidden()

    encoder_optimizer.zero_grad()
    decoder_optimizer.zero_grad()

    input_length = input_tensor.size(0)
    target_length = target_tensor.size(0)

    encoder_outputs = torch.zeros(max_length, encoder.hidden_size, device=device)

    loss = 0

    for ei in range(input_length):
        encoder_output, encoder_hidden = encoder(
            input_tensor[ei], encoder_hidden)
        encoder_outputs[ei] = encoder_output[0, 0]

    decoder_input = torch.tensor([[SOS_token]], device=device)

    decoder_hidden = encoder_hidden

    use_teacher_forcing = True if random.random() < teacher_forcing_ratio else False

    if use_teacher_forcing:
        # Teacher forcing: Feed the target as the next input
        for di in range(target_length):
            decoder_output, decoder_hidden, decoder_attention = decoder(
                decoder_input, decoder_hidden, encoder_outputs)
            loss += criterion(decoder_output, target_tensor[di])
            decoder_input = target_tensor[di]  # Teacher forcing

    else:
        # Without teacher forcing: use its own predictions as the next input
        for di in range(target_length):
            decoder_output, decoder_hidden, decoder_attention = decoder(
                decoder_input, decoder_hidden, encoder_outputs)
            topv, topi = decoder_output.topk(1)
            decoder_input = topi.squeeze().detach()  # detach from history as input

            loss += criterion(decoder_output, target_tensor[di])
            if decoder_input.item() == EOS_token:
                break

    loss.backward()

    encoder_optimizer.step()
    decoder_optimizer.step()

    return loss.item() / target_length

def trainIters(input_lang, output_lang, pairs, device, encoder, decoder, n_iters, print_every=1000, plot_every=100, learning_rate=0.01):
    
    start = time.time()
    plot_losses = []
    print_loss_total = 0  # Reset every print_every
    plot_loss_total = 0  # Reset every plot_every

    encoder_optimizer = optim.SGD(encoder.parameters(), lr=learning_rate)
    decoder_optimizer = optim.SGD(decoder.parameters(), lr=learning_rate)
    training_pairs = [tensorsFromPair(input_lang, output_lang, random.choice(pairs), device)
                      for i in range(n_iters)]
    criterion = nn.NLLLoss()

    for iter in range(1, n_iters + 1):
        training_pair = training_pairs[iter - 1]
        input_tensor = training_pair[0]
        target_tensor = training_pair[1]

        loss = train(device, input_tensor, target_tensor, encoder,
                     decoder, encoder_optimizer, decoder_optimizer, criterion)
        print_loss_total += loss
        plot_loss_total += loss

        if iter % print_every == 0:
            print_loss_avg = print_loss_total / print_every
            print_loss_total = 0
            print('%s (%d %d%%) %.4f' % (timeSince(start, iter / n_iters),
                                         iter, iter / n_iters * 100, print_loss_avg))

        if iter % plot_every == 0:
            plot_loss_avg = plot_loss_total / plot_every
            plot_losses.append(plot_loss_avg)
            plot_loss_total = 0
    
    print("Saving model to {}".format(_ENCODER_MODEL_PATH))
    torch.save(encoder.state_dict(), _ENCODER_MODEL_PATH)
    print("Saving model to {}".format(_DECODER_MODEL_PATH))
    torch.save(decoder.state_dict(), _DECODER_MODEL_PATH)
    # showPlot(plot_losses)


if __name__ == "__main__":
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    print('root path:%s' % _ROOT_DIR)

    vocab = torch.load(_VOCAB_PATH)

    pairs = prepareData(_DATA_PATH)

    hidden_size = 256

    encoder = EncoderRNN(vocab.input_lang.n_words, hidden_size, device).to(device)
    attn_decoder = AttnDecoderRNN(vocab.output_lang.n_words, hidden_size, device, dropout_p=0.1).to(device)

    trainIters(vocab.input_lang, vocab.output_lang, pairs, device, encoder, attn_decoder, 190000, print_every=1000)

    evaluateRandomly(device, vocab.input_lang, vocab.output_lang, pairs, encoder, attn_decoder)
