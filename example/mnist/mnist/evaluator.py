import typing as t
from pathlib import Path

import numpy as np
import torch
from PIL import Image as PILImage
from torchvision import transforms

from starwhale import Image, PipelineHandler, PPLResultIterator, multi_classification
from starwhale.api.service import api, JsonOutput

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class MNISTInference(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

    @api(Image(shape=(28, 28, 3)), JsonOutput())
    def ppl(self, img: Image, **kw: t.Any) -> t.Tuple[float, t.List[float]]:  # type: ignore
        data_tensor = self._pre(img)
        output = self.model(data_tensor)
        return self._post(output)

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def cmp(
        self, ppl_result: PPLResultIterator
    ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["annotations"]["label"])
            result.append(_data["result"][0])
            pr.append(_data["result"][1])
        return label, result, pr

    def _pre(self, input: Image) -> torch.Tensor:
        _tensor = torch.tensor(bytearray(input.to_bytes()), dtype=torch.uint8).reshape(
            input.shape[0], input.shape[1]  # type: ignore
        )
        _image_array = PILImage.fromarray(_tensor.numpy())
        _image = transforms.Compose(
            [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
        )(_image_array)
        return torch.stack([_image]).to(self.device)

    def _post(self, input: torch.Tensor) -> t.Tuple[float, t.List[float]]:
        pred_value = input.argmax(1).item()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix[0]

    def _load_model(self, device: torch.device) -> Net:
        model = Net().to(device)
        model.load_state_dict(
            torch.load(str(ROOTDIR / "models/mnist_cnn.pt"), map_location=device)  # type: ignore
        )
        model.eval()
        print("load mnist model, start to inference...")
        return model
