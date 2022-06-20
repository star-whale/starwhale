import random
import torch
try:
    from .config import EOS_token, SOS_token
    from .dataset import prepareData
    from .helper import MAX_LENGTH, tensorFromSentence
    from .model import AttnDecoderRNN, EncoderRNN
    from .vocab import Vocab, Lang
except ImportError:
    from config import EOS_token, SOS_token
    from dataset import prepareData
    from helper import MAX_LENGTH, tensorFromSentence
    from model import AttnDecoderRNN, EncoderRNN
    from vocab import Vocab, Lang

from nltk.metrics import *
from nltk.translate import *


def evaluate(device, input_lang, output_lang, encoder, decoder, sentence, max_length=MAX_LENGTH):
    with torch.no_grad():
        input_tensor = tensorFromSentence(input_lang, sentence, device)
        input_length = input_tensor.size()[0]
        encoder_hidden = encoder.initHidden()

        encoder_outputs = torch.zeros(max_length, encoder.hidden_size, device=device)

        for ei in range(input_length):
            encoder_output, encoder_hidden = encoder(input_tensor[ei],
                                                     encoder_hidden)
            encoder_outputs[ei] += encoder_output[0, 0]

        decoder_input = torch.tensor([[SOS_token]], device=device)  # SOS

        decoder_hidden = encoder_hidden

        decoded_words = []
        decoder_attentions = torch.zeros(max_length, max_length)

        for di in range(max_length):
            decoder_output, decoder_hidden, decoder_attention = decoder(
                decoder_input, decoder_hidden, encoder_outputs)
            decoder_attentions[di] = decoder_attention.data
            topv, topi = decoder_output.data.topk(1)
            if topi.item() == EOS_token:
                #decoded_words.append('<EOS>')
                break
            else:
                decoded_words.append(output_lang.index2word[topi.item()])

            decoder_input = topi.squeeze().detach()

        return decoded_words, decoder_attentions[:di + 1]

def get_bleu(references, hypotheses):
    # compute BLEU
    bleu_score = bleu([[ref[1:-1]] for ref in references],
                      [hyp[1:-1] for hyp in hypotheses])

    return bleu_score

def evaluate_batch(device, input_lang, output_lang, sentences, encoder, decoder):
    pred_sent = []
    for sentence in sentences:
        #print('>', sentence)

        output_words, attentions = evaluate(device, input_lang, output_lang, encoder, decoder, sentence)
        output_sentence = ' '.join(output_words)
        #print('<', output_sentence)

        pred_sent.append(output_sentence)
    return pred_sent

def evaluateRandomly(device, input_lang, output_lang, pairs, encoder, decoder, n=16):

    for i in range(n):
        pair = random.choice(pairs)
        print('>', pair[0])
        print('=', pair[1])

        output_words, attentions = evaluate(device, input_lang, output_lang, encoder, decoder, pair[0])
        output_sentence = ' '.join(output_words)
        print('<', output_sentence)

        print('score', bleu([pair[1].split()], output_sentence.split()))
        print('')
