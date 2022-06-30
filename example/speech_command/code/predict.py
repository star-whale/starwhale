import torch
import torchaudio
import io

try:
    from . import dataset
except ImportError:
    import dataset

try:
    from . import train
except ImportError:
    import train

try:
    from . import model
except ImportError:
    import model

labels = ['backward', 'bed', 'bird', 'cat', 'dog', 'down', 'eight', 'five',
          'follow', 'forward', 'four', 'go', 'happy', 'house', 'learn', 'left',
          'marvin', 'nine', 'no', 'off', 'on', 'one', 'right', 'seven',
          'sheila', 'six', 'stop', 'three', 'tree', 'two', 'up', 'visual',
          'wow', 'yes', 'zero']


def _load_model(device):
    _model = model.M5(n_input=1, n_output=35)
    _model.load_state_dict(torch.load('../models/m5.pth'))
    _model = _model.to(device)
    _model.eval()
    print("m5  model loaded, start to inference...")
    return _model


def get_likely_index(tensor):
    # find most likely label index for each element in the batch
    return tensor.argmax(dim=-1)


def predict(_model, tensor, device, transform):
    # Use the model to predict the label of the waveform
    tensor = tensor.to(device)
    tensor = transform(tensor)
    tensor = _model(tensor.unsqueeze(0))
    tensor = get_likely_index(tensor)
    print(tensor.squeeze)
    return labels[tensor.squeeze()]


def main():
    device = torch.device('cpu')
    _model = _load_model(device)
    transform = torchaudio.transforms.Resample(orig_freq=16000, new_freq=8000)
    transform = transform.to(device)
    audio_f = open("../data/SpeechCommands/speech_commands_v0.02/right/18f8afd5_nohash_0.wav", "rb")
    _bytes = audio_f.read()
    print(_bytes)
    b = io.BytesIO(_bytes)
    print(type(_bytes))
    test_tensor = torchaudio.load(b)
    print(test_tensor)
    print(predict(_model, test_tensor[0], device, transform))


if __name__ == "__main__":
    main()
