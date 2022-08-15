import os
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torchvision import transforms

from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

from .model import Net

ROOTDIR = Path(__file__).parent.parent
HEIGHT_IMAGE = 32
WIDTH_IMAGE = 32
CHANNEL_IMAGE = 3
ONE_IMAGE_SIZE = CHANNEL_IMAGE * HEIGHT_IMAGE * WIDTH_IMAGE


class CIFAR10Inference(PipelineHandler):
    def __init__(self, device="cpu") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)
        self.model = self._load_model(self.device)

    def ppl(self, data, **kw):
        data = self._pre(data)
        output = self.model(data)
        return self._post(output)

    def handle_label(self, label, **kw):
        return [int(l) for l in label]

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
            _label.extend([int(l) for l in _data[self._label_field]])
            (pred, pr) = _data[self._ppl_data_field]
            _result.extend([int(l) for l in pred])
            _pr.extend([l for l in pr])
        return _label, _result, _pr

    def _pre(self, input: bytes):
        batch_size = 1
        images = []
        from_buffer = np.frombuffer(input, "uint8")
        shape = (1, ONE_IMAGE_SIZE)
        batch_numpy_flatten_data = from_buffer.reshape(shape)
        batch_numpy_flatten_data = np.vstack([batch_numpy_flatten_data]).reshape(
            -1, 3, 32, 32
        )
        batch_numpy_flatten_data = batch_numpy_flatten_data.transpose((0, 2, 3, 1))
        shape_image = (WIDTH_IMAGE, HEIGHT_IMAGE, CHANNEL_IMAGE)
        for i in range(0, batch_size):
            numpy_flatten_data_i_ = batch_numpy_flatten_data[i]
            _image = Image.fromarray(numpy_flatten_data_i_.reshape(shape_image))
            _image = transforms.Compose(
                [
                    transforms.ToTensor(),
                    transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5)),
                ]
            )(_image)
            images.append(_image)
        return torch.stack(images).to(self.device)

    def _post(self, input):
        pred_value = input.argmax(1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(torch.load(str(ROOTDIR / "models/cifar_net.pth")))
        model.eval()
        print("load cifar_net model, start to inference...")
        return model


def load_test_env(fuse=True):
    _p = lambda p: str((ROOTDIR / "test" / p).resolve())

    os.environ["SW_TASK_STATUS_DIR"] = _p("task_volume/status")
    os.environ["SW_TASK_LOG_DIR"] = _p("task_volume/log")
    os.environ["SW_TASK_RESULT_DIR"] = _p("task_volume/result")

    fname = "swds_fuse.json" if fuse else "swds_s3.json"
    # fname = "swds_fuse_simple.json" if fuse else "swds_s3_simple.json"
    os.environ["SW_TASK_INPUT_CONFIG"] = _p(fname)


if __name__ == "__main__":
    load_test_env(fuse=False)
    cifar10 = CIFAR10Inference()
    cifar10._starwhale_internal_run_ppl()
