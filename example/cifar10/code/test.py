from model import Net
import torch
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
from data_slicer import unpickle
import os

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_DATA_PATH = os.path.join(_ROOT_DIR, "../data/cifar-10-batches-py/test_batch")

_batch_size = 4
_transform = transforms.Compose(
    [transforms.ToTensor(),
     transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))])


def transfer(arg_slice):
    slice_size = len(arg_slice)
    arg_slice = np.vstack([arg_slice]).reshape(-1, 3, 32, 32)
    arg_slice = arg_slice.transpose((0, 2, 3, 1))
    images = []
    shape_image = (32, 32, 3)
    for idx in range(0, slice_size):
        numpy_flatten_data_i_ = arg_slice[idx]
        _image = Image.fromarray(numpy_flatten_data_i_.reshape(shape_image))
        _image = _transform(_image)
        images.append(_image)
    return torch.stack(images).to('cpu')


def main():
    content_dict = unpickle(_DATA_PATH)
    all_data = content_dict.get(b'data')
    all_labels = content_dict.get(b'labels')
    data_slice = all_data[0:_batch_size]
    all_labels = all_labels[0:_batch_size]
    data_ready_use = transfer(data_slice)
    model = Net().to('cpu')
    model.load_state_dict(torch.load('../models/cifar_net.pth'))
    out = model(data_ready_use)
    pred = out.argmax(1).flatten().tolist()
    print(pred)
    print(all_labels)


if __name__ == "__main__":
    main()
