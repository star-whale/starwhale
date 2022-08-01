from loguru import logger

import inspect

from starwhale.core.job.model import Context
from starwhale.api.job import step
import torch

from torch.utils.data import DataLoader
import torch.nn.functional as F
from torchvision import datasets, transforms

from .train import train
from .test import test

from .model import Net
import argparse
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torchvision import datasets, transforms
from torch.optim.lr_scheduler import StepLR

# class TestStep:
@step(
    resources="gpu=1,cpu=1",
    concurrency=1,
    task_num=1
)
def pre_train(_context: Context):
    cuda = torch.cuda.is_available()
    device = torch.device("cuda" if cuda else "cpu")
    torch.manual_seed(1)
    dataset = datasets.MNIST(
        "data", train=True, download=True,
        transform=transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize((0.1307,), (0.3081,))
        ])
    )
    gpu_kw = {"num_workers": 1, "pin_memory": True, "shuffle": True} if cuda else {}
    data_loader = DataLoader(dataset, batch_size=60, **gpu_kw)

    model = Net().to(device)
    optimizer = optim.Adadelta(model.parameters(), lr=1.0)
    scheduler = StepLR(optimizer, step_size=1, gamma=0.7)

    print("---> start to train...")
    for epoch in range(1, 5):
        train(model, device, data_loader, optimizer, epoch)
        scheduler.step()

    print("--> save model...")
    torch.save(model.state_dict(), "./models/mnist_cnn.pt")

@step(
    resources="gpu=1,cpu=2",
    concurrency=1,
    task_num=1,
    dependency="pre_train"
)
def evaluate_ppl(_context: Context):
    """
    step 'ppl' demo
    :param _context: common param
    """
    logger.debug("test ppl")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    torch.manual_seed(1)
    dataset = datasets.MNIST(
        "data", train=False, download=True,
        transform=transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize((0.1307,), (0.3081,))
        ])
    )
    data_loader = DataLoader(dataset, batch_size=100)
    model = Net().to(device)
    net = torch.load("./models/mnist_cnn.pt")
    model.load_state_dict(net)
    test(model, device, data_loader)

    # store.set(f"xxx/{context['id']}")


@step(
    resources="cpu=1",
    concurrency=3,
    dependency='evaluate_ppl',
    task_num= 6
)
def evaluate_cmp(_context: Context):
    """
        step 'cmp' demo
        :param _context: common param
        """
    logger.debug("test cmp, index:{}", _context.index)


@step(
    job_name='second',
    resources="gpu=1,cpu=2",
    concurrency=1,
    task_num=2
)
def evaluate_ppl2(_context: Context):
    """
    step 'ppl' demo
    :param _context: common param
    """
    logger.debug("test ppl2")
    # store.set(f"xxx/{context['id']}")


@step(
    job_name='second',
    resources="cpu=1",
    concurrency=1,
    dependency='evaluate_ppl2'
)
def evaluate_cmp2(_context: Context):
    """
        step 'cmp' demo
        :param _context: common param
        """
    logger.debug("test cmp2")



