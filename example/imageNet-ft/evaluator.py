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
from typing import Union, Any, Tuple, List

import numpy as np
from d2l import torch as d2l
import torch
import torchvision
from torch.utils import data as dataset
from torchvision.models import ResNet
from torchvision.models.resnet import BasicBlock

from gradio import gradio
from starwhale.api.service import api
from starwhale import PipelineHandler, Image, multi_classification

ROOTDIR = Path(__file__).parent
_LABEL_NAMES = ["hotdog", "not-hotdog"]
_NUM_CLASSES = len(_LABEL_NAMES)


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

    @torch.no_grad()
    def ppl(self, data: Image, index: Union[int, str], **kw) -> Any:
        output = self.net(data)
        pred_value = output.argmax(1, axis=1).item()
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
            label.append(_data["ds_data"]["label"])
            result.append(_data["result"][0])
            pr.append(_data["result"][1])
        return label, result, pr

    @api(
        gradio.File(), gradio.Label()
    )
    def online_eval(self, content: str):
        _, prob = self.ppl(Image(content))
        return {_LABEL_NAMES[i]: p for i, p in enumerate(prob)}


@torch.no_grad()
def run(fine_tuned: bool = False):
    if fine_tuned:
        net = ResNet(block=BasicBlock, layers=[2, 2, 2, 2], num_classes=2)
        net.load_state_dict(
            torch.load(str(ROOTDIR / "models" / "resnet-ft0.pth"))  # type: ignore
        )
    else:
        net = ResNet(block=BasicBlock, layers=[2, 2, 2, 2], num_classes=1000)
        net.load_state_dict(
            torch.load(str(ROOTDIR / "models" / "resnet18-f37072fd.pth"))  # type: ignore
        )
    net.eval()

    data_dir = ROOTDIR / "data" / "hotdog"
    normalize = torchvision.transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
    test_augs = torchvision.transforms.Compose([
        torchvision.transforms.Resize([256, 256]),
        torchvision.transforms.CenterCrop(224),
        torchvision.transforms.ToTensor(),
        normalize])
    test_iter = dataset.DataLoader(torchvision.datasets.ImageFolder(
        os.path.join(data_dir, 'test'), transform=test_augs), batch_size=128)
    index = 0
    for X, y in test_iter:
        y_res = net(X)
        print(f"index:{index}, res:{y_res}")
        index += 1
        if len(y_res.shape) > 1 and y_res.shape[1] > 1:
            y_res = d2l.argmax(y_res, axis=1)
            print(f"after argmax res:{y_res}")
        cmp = astype(y_res, y.dtype) == y
        cmp_astype = astype(cmp, y.dtype)
        sum = reduce_sum(cmp_astype)
        print(
            f"cmp: {cmp}, cmp_astype:{cmp_astype}, sum:{float(sum)}, y:{y}, y_size:{size(y)}, acc:{float(sum) / size(y)}")

    # test_acc = d2l.evaluate_accuracy(net, test_iter)
    # print(f'test acc {test_acc:.3f}')


astype = lambda x, *args, **kwargs: x.type(*args, **kwargs)
reduce_sum = lambda x, *args, **kwargs: x.sum(*args, **kwargs)
size = lambda x, *args, **kwargs: x.numel(*args, **kwargs)

if __name__ == '__main__':
    run(fine_tuned=True)
