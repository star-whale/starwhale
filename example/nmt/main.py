import os

from nmt.train import train, _ROOT_DIR, build_vocab

if __name__ == "__main__":
    src, target = "eng", "fra"
    train_data_path = os.path.join(_ROOT_DIR, "data", "fra.txt")
    vocab_path = os.path.join(_ROOT_DIR, "models", f"vocab_{src}-{target}.bin")

    print(f"build vocab {src}->{target}...")
    input_lang, output_lang = build_vocab(src, target, vocab_path, train_data_path)

    print("train model...")
    train(input_lang, output_lang, train_data_path)
