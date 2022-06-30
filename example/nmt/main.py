import argparse

import torch

from code import vocab

from code.dataset import prepareData
from code.eval import evaluateRandomly
from code.model import AttnDecoderRNN, EncoderRNN
from code.train import trainIters

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--mode', default='eval', type=str, help='source vocabulary size')

    parser.add_argument('--src_lang', type=str, default='eng', help='file of source sentences')
    parser.add_argument('--tgt_lang', type=str, default='fra', help='file of target sentences')
    
    parser.add_argument('--train_data', default='data/%s-%s.txt', type=str, help='tarin data')
    parser.add_argument('--test_data', default='data/test_%s-%s.txt', type=str, help='test data')

    parser.add_argument('--output', default='models/vocab_%s-%s.bin', type=str, help='output vocabulary file')

    args = parser.parse_args()
    _src_lang = args.src_lang
    _tgt_lang = args.tgt_lang
    _train_data_path = args.train_data % (_src_lang, _tgt_lang)
    _test_data_path = args.test_data % (_src_lang, _tgt_lang)
    _vocab_path = args.output % (_src_lang, _tgt_lang)
    hidden_size = 256
    if args.mode == 'vocab':
        input_lang, output_lang, pairs = vocab.getData(_src_lang, _tgt_lang, False)

        _vocab = vocab.Vocab(input_lang, output_lang)
        print('generated vocabulary, source %d words, target %d words' % (_vocab.input_lang.n_words, _vocab.output_lang.n_words))

        # save
        torch.save(_vocab, _vocab_path )
        print('vocabulary saved to %s' % (_vocab_path))
    elif args.mode == 'train':
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        vocab = torch.load(_vocab_path)

        pairs = prepareData(_train_data_path)

        encoder = EncoderRNN(vocab.input_lang.n_words, hidden_size, device).to(device)
        attn_decoder = AttnDecoderRNN(vocab.output_lang.n_words, hidden_size, device, dropout_p=0.1).to(device)

        print('start to train...')
        trainIters(vocab.input_lang, vocab.output_lang, pairs, device, encoder, attn_decoder, 10000, print_every=1000)

        evaluateRandomly(device, vocab.input_lang, vocab.output_lang, pairs, encoder, attn_decoder)
    elif args.mode == 'eval':
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        vocab = torch.load(_vocab_path)

        pairs = prepareData(_test_data_path)

        encoder = EncoderRNN(vocab.input_lang.n_words, hidden_size, device).to(device)

        net1 = torch.load('models/encoder.pth', device)
        encoder.load_state_dict(net1)

        attn_decoder = AttnDecoderRNN(vocab.output_lang.n_words, hidden_size, device, dropout_p=0.1).to(device)

        net2 = torch.load('models/decoder.pth', device)
        attn_decoder.load_state_dict(net2)

        evaluateRandomly(device, vocab.input_lang, vocab.output_lang, pairs, encoder, attn_decoder)