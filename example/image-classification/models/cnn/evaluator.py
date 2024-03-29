import io
from pathlib import Path

import torch
import gradio
from PIL import Image as PILImage
from torchvision import transforms

from starwhale import Image, PipelineHandler, multi_classification
from starwhale.api.service import api

try:
    from .model import Net
except ImportError:
    from model import Net

ROOTDIR = Path(__file__).parent


class CIFAR10Inference(PipelineHandler):
    def __init__(self) -> None:
        super().__init__(
            predict_log_mode="plain",
            predict_log_dataset_features=["img", "label", "label__classlabel__"],
        )
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

    def predict(self, data):
        img = data.get("img") or data.get("image")
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
    def evaluate(self, ppl_result):
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["input"]["label"])
            result.append(_data["output/pred"])
            pr.append(_data["output/prob"])
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
        return {
            "pred": input.argmax(1).flatten().tolist()[0],
            "prob": torch.nn.Softmax(dim=1)(input).tolist()[0],
        }

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(
            torch.load(str(ROOTDIR / "models" / "cifar_net.pth"), map_location=device)
        )
        model.eval()
        print("load cifar_net model, start to inference...")
        return model

    @api(
        gradio.Image(type="pil"),
        gradio.Label(label="prediction"),
    )
    def online_eval(self, img: PILImage.Image):
        buf = io.BytesIO()
        img.resize((32, 32)).save(buf, format="jpeg")
        classes = (
            "plane",
            "car",
            "bird",
            "cat",
            "deer",
            "dog",
            "frog",
            "horse",
            "ship",
            "truck",
        )
        _, prob = self.predict({"image": Image(fp=buf.getvalue())})
        return {classes[i]: p for i, p in enumerate(prob[0])}
