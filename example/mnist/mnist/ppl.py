from pathlib import Path
import os

import torch
from torchvision import transforms
from PIL import Image

from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

from .model import Net

ROOTDIR = Path(__file__).parent.parent
IMAGE_WIDTH = 28
ONE_IMAGE_SIZE = IMAGE_WIDTH * IMAGE_WIDTH


class MNISTInference(PipelineHandler):

    def __init__(self, device="cpu") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)
        self.model = self._load_model(self.device)

    def ppl(self, data, batch_size, **kw):
        data = self._pre(data, batch_size)
        output = self.model(data)
        return self._post(output)

    def handle_label(self, label, batch_size, **kw):
        return [int(l) for l in label]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        all_labels=[i for i in range(0, 10)]
    )
    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            _label.extend([int(l) for l in _data["label"]])
            _result.extend([int(l) for l in _data["result"]])
        return _label, _result

    def _pre(self, input: bytes, batch_size: int):
        images = []
        for i in range(0, batch_size):
            #TODO: tune for batch transforms
            _start = i * ONE_IMAGE_SIZE
            _tensor = torch.tensor(
                bytearray(input[_start:(_start + ONE_IMAGE_SIZE)]),
                dtype=torch.uint8).reshape(IMAGE_WIDTH, IMAGE_WIDTH)

            _image = Image.fromarray(_tensor.numpy())
            _image = transforms.Compose([
                transforms.ToTensor(),
                transforms.Normalize((0.1307,), (0.3081,))
            ])(_image)

            images.append(_image)
        return torch.stack(images).to(self.device)

    def _post(self, input):
        return input.argmax(1).flatten().tolist()

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(torch.load(str(ROOTDIR / "models/mnist_cnn.pt")))
        model.eval()
        print("load mnist model, start to inference...")
        return model


def load_test_env(fuse=True):
    _p = lambda p : str((ROOTDIR / "test" / p).resolve())

    os.environ["SW_TASK_STATUS_DIR"] = _p("task_volume/status")
    os.environ["SW_TASK_LOG_DIR"] = _p("task_volume/log")
    os.environ["SW_TASK_RESULT_DIR"] = _p("task_volume/result")

    fname = "swds_fuse.json" if fuse else "swds_s3.json"
    #fname = "swds_fuse_simple.json" if fuse else "swds_s3_simple.json"
    os.environ["SW_TASK_INPUT_CONFIG"] = _p(fname)


if __name__ == "__main__":
    load_test_env(fuse=False)
    mnist = MNISTInference()
    mnist._starwhale_internal_run_ppl()