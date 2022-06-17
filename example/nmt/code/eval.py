import os
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


_ROOT_DIR = os.path.dirname(__file__)
_ENCODER_MODEL_PATH = os.path.join(_ROOT_DIR, "models/encoder.pth")
_DECODER_MODEL_PATH = os.path.join(_ROOT_DIR, "models/decoder.pth")
_EVAL_RESULT_PATH = os.path.join(_ROOT_DIR, "results/pred.txt")
_EVAL_LABEL_PATH = os.path.join(_ROOT_DIR, "results/label.txt")

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
    #input_sent = []
    #output_sent = []
    #pred_sent = []
    for i in range(n):
        output_sent = []
        pred_sent = []
        pair = random.choice(pairs)
        print('>', pair[0])
        print('=', pair[1])
        output_sent.append(pair[1])

        append_new_line(_EVAL_LABEL_PATH, pair[1])

        output_words, attentions = evaluate(device, input_lang, output_lang, encoder, decoder, pair[0])
        output_sentence = ' '.join(output_words)
        print('<', output_sentence)

        pred_sent.append(output_sentence)
        append_new_line(_EVAL_RESULT_PATH, output_sentence)

        # valid_metric = bleu(pair[1].split(), output_sentence.split())
        print('score', bleu([pair[1].split()], output_sentence.split()))
        # print('accuracy', accuracy(pair[1].split(), output_sentence.split()))
        print('')
    # bleu scroe
    # data = zip(input_sent, output_sent)

    # valid_metric = get_bleu([tgt for src, tgt in data], pred_sent)
    # print('score', valid_metric)

    #reference = [['the', 'quick', 'brown', 'fox', 'jumped', 'over', 'the', 'lazy', 'dog']]
    reference = [['ils', 'parlent', 'rarement', 'francais', 'si', 'tant', 'est', 'qu', 'ils', 'le', 'fassent','.']]
    #candidate = ['the', 'quick', 'brown', 'fox', 'jumped', 'over', 'the', 'lazy', 'dog']
    candidate = ['ils', 'se', 'rarement', 'jamais', 'francais', '.']
    score = bleu(reference, candidate)
    print('standard', score)


def append_new_line(file_name, text_to_append):
    """Append given text as a new line at the end of file"""
    # Open the file in append & read mode ('a+')
    with open(file_name, "a+") as file_object:
        # Move read cursor to the start of file.
        file_object.seek(0)
        # If file is not empty then append '\n'
        data = file_object.read(100)
        if len(data) > 0:
            file_object.write("\n")
        # Append text at the end of file
        file_object.write(text_to_append)

if __name__ == "__main__":
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    vocab = torch.load('data/vocab_eng-fra.bin')

    pairs = prepareData('data/test_eng-fra.txt')


    hidden_size = 256
    encoder1 = EncoderRNN(vocab.input_lang.n_words, hidden_size, device).to(device)

    net1 = torch.load(_ENCODER_MODEL_PATH, device)
    encoder1.load_state_dict(net1)


    attn_decoder1 = AttnDecoderRNN(vocab.output_lang.n_words, hidden_size, device, dropout_p=0.1).to(device)

    net2 = torch.load(_DECODER_MODEL_PATH, device)
    attn_decoder1.load_state_dict(net2)

    evaluateRandomly(device, vocab.input_lang, vocab.output_lang, pairs, encoder1, attn_decoder1)