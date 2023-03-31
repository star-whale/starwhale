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

from d2l import torch as d2l
import torch
import torchvision
from torch import nn
from torch.utils import data
from torchvision.models import ResNet
from torchvision.models.resnet import BasicBlock

from starwhale import step, model, dataset
from starwhale.utils.fs import ensure_dir
from evaluator import ImageNetEvaluation

ROOTDIR = Path(__file__).parent.parent


@step(name="resnet-fine-tuning")
def run(learning_rate=5e-5, batch_size=128, num_epochs=5, param_group=True):
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

    train_augs = torchvision.transforms.Compose([
        torchvision.transforms.RandomResizedCrop(224),
        torchvision.transforms.RandomHorizontalFlip(),
        torchvision.transforms.ToTensor(),
        normalize])

    test_augs = torchvision.transforms.Compose([
        torchvision.transforms.Resize([256, 256]),
        torchvision.transforms.CenterCrop(224),
        torchvision.transforms.ToTensor(),
        normalize])

    dataset

    # dataset todo: use sw datasets?!
    data_dir = ROOTDIR / "data"
    train_iter = data.DataLoader(torchvision.datasets.ImageFolder(
        os.path.join(data_dir, 'train'), transform=train_augs), batch_size=batch_size, shuffle=True)
    test_iter = data.DataLoader(torchvision.datasets.ImageFolder(
        os.path.join(data_dir, 'test'), transform=test_augs), batch_size=batch_size)
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
    d2l.train_ch13(finetune_net, train_iter, test_iter, loss, trainer, num_epochs)
    # todo use tmp or via sw deal?
    ft_dir = ROOTDIR / "models"
    ensure_dir(ft_dir)
    torch.save(finetune_net.state_dict(), ft_dir / "resnet-ft.pth")
    model.build(evaluation_handler=ImageNetEvaluation, push_to="cloud://server/project/starwhale")


def build_model():
    model.build(evaluation_handler=ImageNetEvaluation, push_to="cloud://server/project/starwhale")


if __name__ == '__main__':
    build_model()
