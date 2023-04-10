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
from pathlib import Path
from typing import Any, Dict

import numpy as np
import torch
import torchvision
from d2l import torch as d2l
from starwhale.api import experiment, model
from starwhale.api.service import api
from torch import nn
from torch.utils import data
from torchvision.models import ResNet
from torchvision.models.resnet import BasicBlock
from torchvision.transforms import Compose

from gradio import gradio
from starwhale import (Context, Image, PipelineHandler, dataset,
                       multi_classification, pass_context)

ROOTDIR = Path(__file__).parent.parent
_LABEL_NAMES = ["hotdog", "not-hotdog"]
_NUM_CLASSES = len(_LABEL_NAMES)


class SwCompose(Compose):
    def __call__(self, img):
        # todo: TypeError: cannot pickle '_thread.lock' object
        # img = deepcopy(img) #error
        # todo: refactor sw object
        _img = img.get("img").to_pil()
        for t in self.transforms:
            _img = t(_img)
        # todo: RuntimeError: <function Dataset.__setitem__ at 0x7f12e2efc430> does not work in the readonly mode
        # img["img"] = _img #error
        return _img, torch.tensor(
            _LABEL_NAMES.index(img.get("label")), dtype=torch.long
        )


@torch.no_grad()
def evaluate_accuracy(net, data_iter, device=None):
    """Compute the accuracy for a model on a dataset using a GPU.

    Defined in :numref:`sec_lenet`"""
    if isinstance(net, nn.Module):
        net.eval()  # Set the model to evaluation mode
        if not device:
            device = next(iter(net.parameters())).device
    # No. of correct predictions, no. of predictions
    metric = d2l.Accumulator(2)

    for X, y in data_iter:
        if isinstance(X, list):
            # Required for BERT Fine-tuning (to be covered later)
            X = [x.to(device) for x in X]
        else:
            X = X.to(device)
        y = y.to(device)
        metric.add(d2l.accuracy(net(X), y), d2l.size(y))
    return metric[0] / metric[1]


@pass_context
@experiment.fine_tune()
def fine_tune(
    context: Context,
    learning_rate=5e-5,
    batch_size=128,
    num_epochs=10,
    param_group=True,
):
    # init
    finetune_net = ResNet(BasicBlock, [2, 2, 2, 2])
    # load from pretrained model todo: load form existed sw model package(sdk)
    finetune_net.load_state_dict(
        torch.load(str(ROOTDIR / "models" / "resnet18.pth"))  # type: ignore
    )
    # reset to 2 class
    finetune_net.fc = nn.Linear(finetune_net.fc.in_features, 2)
    nn.init.xavier_uniform_(finetune_net.fc.weight)

    normalize = torchvision.transforms.Normalize(
        [0.485, 0.456, 0.406], [0.229, 0.224, 0.225]
    )

    train_augs = SwCompose(
        [
            torchvision.transforms.RandomResizedCrop(224),
            torchvision.transforms.RandomHorizontalFlip(),
            torchvision.transforms.ToTensor(),
            normalize,
        ]
    )

    # use starwhale dataset
    train_dataset = dataset(context.dataset_uris[0], readonly=True, create="forbid")
    train_iter = data.DataLoader(
        train_dataset.to_pytorch(transform=train_augs), batch_size=batch_size
    )

    loss = nn.CrossEntropyLoss(reduction="none")
    if param_group:
        params_1x = [
            param
            for name, param in finetune_net.named_parameters()
            if name not in ["fc.weight", "fc.bias"]
        ]
        trainer = torch.optim.SGD(
            [
                {"params": params_1x},
                {"params": finetune_net.fc.parameters(), "lr": learning_rate * 10},
            ],
            lr=learning_rate,
            weight_decay=0.001,
        )
    else:
        trainer = torch.optim.SGD(
            finetune_net.parameters(), lr=learning_rate, weight_decay=0.001
        )

    devices = d2l.try_all_gpus()

    # train(finetune_net, train_iter, loss, trainer, num_epochs)
    timer, num_batches = d2l.Timer(), len(train_iter)
    animator = d2l.Animator(
        xlabel="epoch",
        xlim=[1, num_epochs],
        ylim=[0, 1],
        legend=["train loss", "train acc", "test acc"],
    )
    net = nn.DataParallel(finetune_net, device_ids=devices).to(devices[0])
    for epoch in range(num_epochs):
        # Sum of training loss, sum of training accuracy, no. of examples,
        # no. of predictions
        metric = d2l.Accumulator(4)
        for i, (features, labels) in enumerate(train_iter):
            timer.start()
            l, acc = train_batch(net, features, labels, loss, trainer, devices)
            metric.add(l, acc, labels.shape[0], labels.numel())
            timer.stop()
            if (i + 1) % (num_batches // 5) == 0 or i == num_batches - 1:
                animator.add(
                    epoch + (i + 1) / num_batches,
                    (metric[0] / metric[2], metric[1] / metric[3], None),
                )
        print(
            f"epoch[{epoch}] loss {metric[0] / metric[2]:.3f}, train acc {metric[1] / metric[3]:.3f}"
        )
        print(
            f"epoch[{epoch}] {metric[2] * num_epochs / timer.sum():.1f} examples/sec on {str(devices)}"
        )

    # save and build model
    torch.save(finetune_net.state_dict(), ROOTDIR / "models" / "resnet-ft.pth")
    # todo: the name of ft model can be set by env
    model.build(
        workdir=ROOTDIR,
        name="imageNet-for-hotdog",
        evaluation_handler=ImageNetEvaluation,
    )


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


# todo need some hook for user
class ImageNetEvaluation(PipelineHandler):
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.net = ResNet(block=BasicBlock, layers=[2, 2, 2, 2], num_classes=2)
        self.net.load_state_dict(
            torch.load(str(ROOTDIR / "models" / "resnet-ft.pth"))  # type: ignore
        )
        self.net.eval()
        super().__init__()

    def _pre(self, data: Image) -> torch.Tensor:
        normalize = torchvision.transforms.Normalize(
            [0.485, 0.456, 0.406], [0.229, 0.224, 0.225]
        )

        test_augs = Compose(
            [
                torchvision.transforms.Resize([256, 256]),
                torchvision.transforms.CenterCrop(224),
                torchvision.transforms.ToTensor(),
                normalize,
            ]
        )
        return torch.stack([test_augs(data.to_pil())]).to(self.device)

    @torch.no_grad()
    def ppl(self, data: Dict[str, Any], **kw) -> Any:
        output = self.net(self._pre(data.get("img")))
        pred_value = output.argmax(1).item()
        probability_matrix = np.exp(output.tolist()).tolist()
        return pred_value, probability_matrix[0]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, _NUM_CLASSES)],
    )
    def cmp(self, ppl_result):
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_LABEL_NAMES.index(_data["ds_data"]["label"]))
            result.append(_data["result"][0])
            pr.append(_data["result"][1])
        return label, result, pr

    @api(gradio.File(), gradio.Label())
    def online_eval(self, file: Any):
        with open(file.name, "rb") as f:
            data = Image(f.read(), shape=(28, 28, 1))
        _, prob = self.ppl({"img": data})
        return {_LABEL_NAMES[i]: p for i, p in enumerate(prob)}
