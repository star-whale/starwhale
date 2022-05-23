from model import Net
import torch
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
from data_slicer import unpickle

DATA_PATH = '/home/anda/starwhale_code/example/cifar10/data/cifar-10-batches-py/test_batch'

batch_size = 4
transform = transforms.Compose(
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
        _image = transform(_image)
        images.append(_image)
    return torch.stack(images).to('cpu')


content_dict = unpickle(DATA_PATH)

all_data = content_dict.get(b'data')
# print(len(all_data))
# all_data = np.vstack([all_data]).reshape(-1, 3, 32, 32)
# print(len(all_data))
# all_data = all_data.transpose((0, 2, 3, 1))
# print(len(all_data))
all_labels = content_dict.get(b'labels')

data_slice = all_data[0:batch_size]
# data_slice = data_transfer(data_slice)
# print(len(data_slice))
all_labels = all_labels[0:batch_size]

data_ready_use = transfer(data_slice)

model = Net().to('cpu')
model.load_state_dict(torch.load('../models/cifar_net.pth'))
out = model(data_ready_use)
pred = out.argmax(1).flatten().tolist()
print(pred)
print(all_labels)
