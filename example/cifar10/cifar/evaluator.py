import io
from pathlib import Path

import numpy as np
import torch
from PIL import Image as PILImage
from torchvision import transforms

from starwhale import Image, PipelineHandler, multi_classification

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class CIFAR10Inference(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

    def ppl(self, data: dict, **kw):
        data_tensor = self._pre(data["image"])
        output = self.model(data_tensor)
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
            label.append(_data["ds_data"]["label"])
            result.extend(_data["result"][0])
            pr.extend(_data["result"][1])
        return label, result, pr

    def _pre(self, input: Image) -> torch.Tensor:
        _image = PILImage.open(io.BytesIO(input.to_bytes()))
        _image = transforms.Compose(
            [
                transforms.ToTensor(),
                transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5)),
            ]
        )(_image)
        return torch.stack([_image]).to(self.device)

    def _post(self, input):
        pred_value = input.argmax(1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(
            torch.load(str(ROOTDIR / "models" / "cifar_net.pth"), map_location=device)
        )
        model.eval()
        print("load cifar_net model, start to inference...")
        return model
