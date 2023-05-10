import typing as t
from pathlib import Path

import numpy as np
import torch
import gradio
from PIL import Image as PILImage
from torchvision import transforms

from starwhale import Image
from starwhale import model as starwhale_model
from starwhale import evaluation, multi_classification
from starwhale.api.service import api

try:
    from .model import Net
except ImportError:
    from model import Net  # type: ignore

ROOTDIR = Path(__file__).parent.parent

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model: t.Optional[Net] = None


@evaluation.predict(
    resources={"memory": 200},
    concurrency=2,
    replicas=4,
    batch_size=1,
    fail_on_error=False,
    auto_log=True,
)
def predict_image(data: t.Dict) -> t.Any:
    img: Image = data["img"]
    _tensor = torch.tensor(bytearray(img.to_bytes()), dtype=torch.uint8).reshape(
        img.shape[0], img.shape[1]  # type: ignore
    )
    _tensor = transforms.Compose(
        [transforms.ToTensor(), transforms.transforms.Normalize((0.1307,), (0.3081,))]
    )(PILImage.fromarray(_tensor.numpy()))
    _tensor = torch.stack([_tensor]).to(device)

    global model
    if model is None:
        model = load_model()

    result: torch.Tensor = model(_tensor)
    pred_value = result.argmax(1).item()
    probability_matrix = np.exp(result.tolist()).tolist()
    return pred_value, probability_matrix[0]


@evaluation.evaluate(use_predict_auto_log=True, needs=[predict_image])
@multi_classification(
    confusion_matrix_normalize="all",
    show_hamming_loss=True,
    show_cohen_kappa_score=True,
    show_roc_auc=True,
    all_labels=[i for i in range(0, 10)],
)
def evaluate_results(predict_result_iter: t.Iterator) -> t.Tuple:
    result, label, pr = [], [], []
    for _data in predict_result_iter:
        label.append(_data["ds_data"]["label"])
        result.append(_data["result"][0])
        pr.append(_data["result"][1])
    return label, result, pr


@api(gradio.File(), gradio.Label())
def predict_view(file: t.Any) -> t.Any:
    with open(file.name, "rb") as f:
        data = Image(f.read(), shape=(28, 28, 1))
    _, prob = predict_image({"img": data})
    return {i: p for i, p in enumerate(prob)}


def load_model() -> Net:
    model = Net().to(device)
    model.load_state_dict(
        torch.load(  # type: ignore
            str(ROOTDIR / "models" / "mnist_cnn.pt"),
            map_location=device,
        )
    )
    model.eval()
    return model


if __name__ == "__main__":
    # use imported modules as search modules
    starwhale_model.build(workdir=ROOTDIR)

    # use function object as search modules
    #    starwhale_model.build(
    #        name="mnist",
    #        workdir=ROOTDIR,
    #        modules=[predict_image, evaluate_results],
    #    )

    # use import-path str as search modules
    # starwhale_model.build(
    #    name="mnist",
    #    workdir=ROOTDIR,
    #    modules=["mnist.plain_evaluator"],
    # )
