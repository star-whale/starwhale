from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torchvision import transforms

from starwhale.api.job import Context
from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

from .model import Net

ROOTDIR = Path(__file__).parent.parent
HEIGHT_IMAGE = 32
WIDTH_IMAGE = 32
CHANNEL_IMAGE = 3
ONE_IMAGE_SIZE = CHANNEL_IMAGE * HEIGHT_IMAGE * WIDTH_IMAGE


class CIFAR10Inference(PipelineHandler):
    def __init__(self, context: Context) -> None:
        super().__init__(context=context)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

    def ppl(self, data, **kw):
        data = self._pre(data)
        output = self.model(data)
        return self._post(output)

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def cmp(self, ppl_result):
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["annotations"]["label"])
            (pred, pr) = _data["result"]
            result.extend(pred)
            pr.extend(pr)
        return label, result, pr

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
