import argparse

import torch

from code import vocab

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--src_vocab_size', default=500000, type=int, help='source vocabulary size')
    parser.add_argument('--tgt_vocab_size', default=500000, type=int, help='target vocabulary size')
    parser.add_argument('--include_singleton', action='store_true', default=False, help='whether to include singleton'
                                                                                        'in the vocabulary (default=False)')

    parser.add_argument('--src_lang', type=str, default='eng', help='file of source sentences')
    parser.add_argument('--tgt_lang', type=str, default='fra', help='file of target sentences')

    parser.add_argument('--train_src', type=str, default='data/train.de-en.en', help='file of source sentences')
    parser.add_argument('--train_tgt', type=str, default='data/train.de-en.de', help='file of target sentences')

    parser.add_argument('--output', default='models/vocab_%s-%s.bin', type=str, help='output vocabulary file')

    args = parser.parse_args()
    src_lang = args.src_lang
    tgt_lang = args.tgt_lang

    input_lang, output_lang, pairs = vocab.getData(src_lang, tgt_lang, False)

    _vocab = vocab.Vocab(input_lang, output_lang)
    print('generated vocabulary, source %d words, target %d words' % (_vocab.input_lang.n_words, _vocab.output_lang.n_words))

    # save
    torch.save(_vocab, args.output % (src_lang, tgt_lang) )
    print('vocabulary saved to %s' % (args.output % (src_lang, tgt_lang)))