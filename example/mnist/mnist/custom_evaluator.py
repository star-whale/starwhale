from __future__ import annotations

import typing as t
from pathlib import Path

import dill
import numpy as np
import torch
import gradio
from PIL import Image as PILImage
from torchvision import transforms

from starwhale import (
    Image,
    Context,
    dataset,
    handler,
    evaluation,
    pass_context,
    multi_classification,
)
from starwhale.api.service import api
from starwhale.base.uri.resource import Resource, ResourceType

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class CustomPipelineHandler:
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)
        self.batch_size = 5

    @handler(replicas=2, name="ppl")
    @pass_context
    def run_ppl(self, context: Context) -> None:
        print(f"start to run ppl@{context.version}-{context.total}-{context.index}...")

        for uri_str in context.dataset_uris:
            _uri = Resource(uri_str, typ=ResourceType.dataset)
            ds = dataset(_uri)
            ds.make_distributed_consumption(session_id=context.version)
            for rows in ds.batch_iter(self.batch_size):
                pred_values, probability_matrixs = self.batch_ppl([r[1] for r in rows])
                for (
                    (_idx, _data),
                    pred_value,
                    probability_matrix,
                ) in zip(rows, pred_values, probability_matrixs):
                    _unique_id = f"{_uri.version}_{_idx}"

                    evaluation.log(
                        category="results",
                        id=_unique_id,
                        metrics=dict(
                            pred_value=dill.dumps(pred_value),
                            probability_matrix=dill.dumps(probability_matrix),
                            label=_data["label"],
                        ),
                    )

    @handler(needs=[run_ppl], name="cmp")
    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def run_cmp(self) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
        result, label, pr = [], [], []
        for data in evaluation.iter("results"):
            result.append(dill.loads(data["pred_value"]))
            label.append(data["label"])
            pr.append(dill.loads(data["probability_matrix"]))
        return label, result, pr

    def _load_model(self, device: torch.device) -> Net:
        model = Net().to(device)
        model.load_state_dict(
            torch.load(str(ROOTDIR / "models/mnist_cnn.pt"), map_location=device)  # type: ignore
        )
        model.eval()
        print("load mnist model, start to inference...")
        return model

    def _pre(self, data_items: t.List[t.Dict]) -> t.Any:
        images = []
        for data in data_items:
            _tensor = torch.tensor(
                bytearray(data["img"].to_bytes()), dtype=torch.uint8
            ).reshape(
                data["img"].shape[0], data["img"].shape[1]  # type: ignore
            )
            _image_array = PILImage.fromarray(_tensor.numpy())
            _image = transforms.Compose(
                [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
            )(_image_array)

            images.append(_image)
        return torch.stack(images).to(self.device)

    def ppl(
        self, data: t.Dict, **kw: t.Any
    ) -> t.Tuple[t.List[int], t.List[t.List[float]]]:
        return self.batch_ppl([data])

    def batch_ppl(
        self, images: t.List[t.Dict], **kw: t.Any
    ) -> t.Tuple[t.List[int], t.List[t.List[float]]]:
        data_tensor = self._pre(images)
        output = self.model(data_tensor)
        return output.argmax(1).flatten().tolist(), np.exp(output.tolist()).tolist()

    @api(
        inputs=gradio.Sketchpad(shape=(28, 28), image_mode="L"), outputs=gradio.Label()
    )
    def draw(self, data: np.ndarray) -> t.Any:
        _image_array = PILImage.fromarray(data.astype("int8"), mode="L")
        _image = transforms.Compose(
            [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
        )(_image_array)
        output = self.model(torch.stack([_image]).to(self.device))
        return {i: p for i, p in enumerate(np.exp(output.tolist()).tolist()[0])}

    @api(inputs=gradio.File(), outputs=gradio.Label())
    def upload_bin_file(self, file: t.Any) -> t.Any:
        with open(file.name, "rb") as f:
            data = Image(f.read(), shape=(28, 28, 1))
        _, prob = self.ppl({"img": data})
        return {i: p for i, p in enumerate(prob[0])}
