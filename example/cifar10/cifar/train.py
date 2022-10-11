import os

import torch
import torch.nn as nn
import torch.optim as optim
import torchvision
import torchvision.transforms as transforms
from model import Net
from torch.optim.lr_scheduler import StepLR

# https://pytorch.org/tutorials/beginner/blitz/cifar10_tutorial.html
_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_MODEL_PATH = os.path.join(_ROOT_DIR, "models/cifar_net.pth")
_DATA_DIR = os.path.join(_ROOT_DIR, "data")


def train():
    transform = transforms.Compose(
        [transforms.ToTensor(), transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))]
    )
    batch_size = 4
    train_set = torchvision.datasets.CIFAR10(
        root=_DATA_DIR, train=True, download=True, transform=transform
    )
    train_loader = torch.utils.data.DataLoader(
        train_set, batch_size=batch_size, shuffle=True, num_workers=2
    )
    net = Net()
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.SGD(net.parameters(), lr=0.001, momentum=0.9)
    scheduler = StepLR(optimizer, step_size=1, gamma=0.7)
    for epoch in range(10):  # loop over the dataset multiple times
        running_loss = 0.0
        for i, data in enumerate(train_loader, 0):
            # get the inputs; data is a list of [inputs, labels]
            inputs, labels = data
            # zero the parameter gradients
            optimizer.zero_grad()
            # forward + backward + optimize
            outputs = net(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            running_loss += loss.item()
            if i % 2000 == 1999:  # print every 2000 mini-batches
                print(f"[{epoch + 1}, {i + 1:5d}] loss: {running_loss / 2000:.3f}")
                running_loss = 0.0
        scheduler.step()

    print("Finished Training")
    torch.save(net.state_dict(), _MODEL_PATH)


if __name__ == "__main__":
    train()
