#  Copyright 2022 Starwhale, Inc. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import os
from pathlib import Path
from typing import Optional

from d2l import torch as d2l
import torch
import torchvision
from torch import nn
from torch.utils import data
from torchvision.models import ResNet
from torchvision.models.resnet import BasicBlock

from starwhale import model, dataset
from starwhale.consts.env import SWEnv
from pipeline import ImageNetEvaluation, SwCompose

ROOTDIR = Path(__file__).parent.parent
_LABEL_NAMES = ["hotdog", "not-hotdog"]


def run(learning_rate=5e-5,
        batch_size=128,
        num_epochs=5,
        param_group=True,
        remote_instance_uri: Optional[str] = None,
        remote_project: Optional[str] = None,
        ):
    # init
    finetune_net = ResNet(BasicBlock, [2, 2, 2, 2])
    # load from pretrained model todo: load form existed sw model package(sdk)
    finetune_net.load_state_dict(
        torch.load(str(ROOTDIR / "models" / "resnet18-f37072fd.pth"))  # type: ignore
    )
    # reset to 2 class
    finetune_net.fc = nn.Linear(finetune_net.fc.in_features, 2)
    nn.init.xavier_uniform_(finetune_net.fc.weight)

    normalize = torchvision.transforms.Normalize(
        [0.485, 0.456, 0.406], [0.229, 0.224, 0.225])

    train_augs = SwCompose([
        torchvision.transforms.RandomResizedCrop(224),
        torchvision.transforms.RandomHorizontalFlip(),
        torchvision.transforms.ToTensor(),
        normalize])

    test_augs = SwCompose([
        torchvision.transforms.Resize([256, 256]),
        torchvision.transforms.CenterCrop(224),
        torchvision.transforms.ToTensor(),
        normalize])

    # use starwhale dataset
    remote_instance_uri = remote_instance_uri or os.getenv(SWEnv.instance_uri)
    remote_project = remote_project or os.getenv(SWEnv.project)
    server_pro_uri = f"{remote_instance_uri}/project/{remote_project}"
    # server_pro_uri = "local/project/self"
    # todo: this should control datastore write, but not control user rewrite the obj?
    test_dataset = dataset(f"{server_pro_uri}/dataset/hotdog_test/version/latest", readonly=True)
    train_dataset = dataset(f"{server_pro_uri}/dataset/hotdog_train/version/latest", readonly=True)
    #  todo: support batch
    #  todo: support wrapper transform()
    test_iter = data.DataLoader(test_dataset.to_pytorch(transform=test_augs), batch_size=batch_size)
    train_iter = data.DataLoader(train_dataset.to_pytorch(transform=train_augs), batch_size=batch_size)

    # use original dataset
    # data_dir = ROOTDIR / "data"
    # train_iter = data.DataLoader(torchvision.datasets.ImageFolder(
    #     os.path.join(data_dir, 'train'), transform=train_augs), batch_size=batch_size, shuffle=True)
    # test_iter = data.DataLoader(torchvision.datasets.ImageFolder(
    #     os.path.join(data_dir, 'test'), transform=test_augs), batch_size=batch_size)
    loss = nn.CrossEntropyLoss(reduction="none")
    if param_group:
        params_1x = [param for name, param in finetune_net.named_parameters()
                     if name not in ["fc.weight", "fc.bias"]]
        trainer = torch.optim.SGD([{'params': params_1x},
                                   {'params': finetune_net.fc.parameters(),
                                    'lr': learning_rate * 10}],
                                  lr=learning_rate, weight_decay=0.001)
    else:
        trainer = torch.optim.SGD(finetune_net.parameters(), lr=learning_rate,
                                  weight_decay=0.001)
    # todo: could not compatible native pytorch program
    # d2l.train_ch13(finetune_net, train_iter, test_iter, loss, trainer, num_epochs)
    train(finetune_net, train_iter, test_iter, loss, trainer, num_epochs)

    torch.save(finetune_net.state_dict(), ROOTDIR / "models" / "resnet-ft.pth")
    model.build(workdir=ROOTDIR, evaluation_handler=ImageNetEvaluation)


def train_batch(net, X, y, loss, trainer, devices):
    """Train for a minibatch with mutiple GPUs (defined in Chapter 13).

    Defined in :numref:`sec_image_augmentation`"""
    if isinstance(X, list):
        # Required for BERT fine-tuning (to be covered later)
        X = [x.to(devices[0]) for x in X]
    else:
        X = X.to(devices[0])
    # y = y.to(devices[0])
    net.train()
    trainer.zero_grad()
    pred = net(X)
    l = loss(pred, y)
    l.sum().backward()
    trainer.step()
    train_loss_sum = l.sum()
    train_acc_sum = d2l.accuracy(pred, y)
    return train_loss_sum, train_acc_sum


def train(net, train_iter, test_iter, loss, trainer, num_epochs,
          devices=d2l.try_all_gpus()):
    """Train a model with mutiple GPUs (defined in Chapter 13).

    Defined in :numref:`sec_image_augmentation`"""
    timer, num_batches = d2l.Timer(), len(train_iter)
    animator = d2l.Animator(xlabel='epoch', xlim=[1, num_epochs], ylim=[0, 1],
                            legend=['train loss', 'train acc', 'test acc'])
    net = nn.DataParallel(net, device_ids=devices).to(devices[0])
    for epoch in range(num_epochs):
        # Sum of training loss, sum of training accuracy, no. of examples,
        # no. of predictions
        metric = d2l.Accumulator(4)
        for i, (features, labels) in enumerate(train_iter):
            timer.start()
            l, acc = train_batch(
                net, features, labels, loss, trainer, devices)
            metric.add(l, acc, labels.shape[0], labels.numel())
            timer.stop()
            if (i + 1) % (num_batches // 5) == 0 or i == num_batches - 1:
                animator.add(epoch + (i + 1) / num_batches,
                             (metric[0] / metric[2], metric[1] / metric[3],
                              None))
        # test_acc = evaluate_accuracy_gpu(net, test_iter)
        # animator.add(epoch + 1, (None, None, test_acc))
        print(f'epoch[{epoch}] loss {metric[0] / metric[2]:.3f}, train acc {metric[1] / metric[3]:.3f}')
        print(f'epoch[{epoch}] {metric[2] * num_epochs / timer.sum():.1f} examples/sec on {str(devices)}')


if __name__ == '__main__':
    os.environ.setdefault(SWEnv.instance_uri, "cloud://server")
    os.environ.setdefault(SWEnv.project, "starwhale")
    model.build(workdir=ROOTDIR, evaluation_handler=ImageNetEvaluation)
