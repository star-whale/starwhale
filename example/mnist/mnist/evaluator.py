import typing as t
from pathlib import Path

import numpy as np
import torch
import gradio
from PIL import Image as PILImage
from torchvision import transforms

from starwhale import Image, PipelineHandler, multi_classification
from starwhale.api.service import api

try:
    from .model import Net
except ImportError:
    from model import Net  # type: ignore

ROOTDIR = Path(__file__).parent.parent


class MNISTInference(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

    @PipelineHandler.run(replicas=1, resources={"memory": 200 * 1024})
    def predict(self, data: t.Dict[str, t.Any]) -> t.Tuple[float, t.List[float]]:  # type: ignore
        data_tensor = self._pre(data["img"])
        output = self.model(data_tensor)
        return self._post(output)

    @api(gradio.File(), gradio.Label())
    def upload_bin_file(self, file: t.Any) -> t.Any:
        with open(file.name, "rb") as f:
            data = Image(f.read(), shape=(28, 28, 1))
        _, prob = self.predict({"img": data})
        return {i: p for i, p in enumerate(prob)}

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    @PipelineHandler.run(resources={"memory": 200 * 1024})
    def evaluate(
        self, ppl_result: t.Iterator
    ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["input"]["label"])
            result.append(_data["output"][0])
            pr.append(_data["output"][1])
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
