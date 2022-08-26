import os
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torchvision import transforms

from starwhale.api.job import Context
from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

try:
    from .model import Net
except ImportError:
    from model import Net

ROOTDIR = Path(__file__).parent.parent
IMAGE_WIDTH = 28
ONE_IMAGE_SIZE = IMAGE_WIDTH * IMAGE_WIDTH


class MNISTInference(PipelineHandler):
    def __init__(self, context: Context, device="cpu") -> None:
        super().__init__(context=context, merge_label=True, ignore_error=True)
        self.device = torch.device(device)
        self.model = self._load_model(self.device)

    def ppl(self, data, **kw):
        data = self._pre(data)
        output = self.model(data)
        return self._post(output)

    def handle_label(self, label, **kw):
        return int(label)

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def cmp(self, _data_loader):
        _result, _label, _pr = [], [], []
        for _data in _data_loader:
            _label.append(_data[self._label_field])
            # unpack data according to the return value of function ppl
            (pred, pr) = _data[self._ppl_data_field]
            _result.extend(pred)
            _pr.extend(pr)
        return _label, _result, _pr

    def _pre(self, input: bytes):
        _tensor = torch.tensor(bytearray(input), dtype=torch.uint8).reshape(
            IMAGE_WIDTH, IMAGE_WIDTH
        )
        _image_array = Image.fromarray(_tensor.numpy())
        _image = transforms.Compose(
            [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
        )(_image_array)
        return torch.stack([_image]).to(self.device)

    def _post(self, input):
        pred_value = input.argmax(1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(torch.load(str(ROOTDIR / "models/mnist_cnn.pt")))
        model.eval()
        print("load mnist model, start to inference...")
        return model


if __name__ == "__main__":
    from starwhale.api.job import Context

    context = Context(
        workdir=Path("."),
        dataset_uris=["mnist/version/latest"],
        project="self",
        version="latest",
        kw={
            "status_dir": "/tmp/mnist/status",
            "log_dir": "/tmp/mnist/log",
        },
    )
    mnist = MNISTInference(context)
    mnist._starwhale_internal_run_ppl()
