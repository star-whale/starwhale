import typing as t
from pathlib import Path

import numpy as np
import torch
import gradio
from PIL import Image as PILImage
from loguru import logger
from torchvision import transforms

from starwhale import (
    URI,
    step,
    Image,
    Context,
    dataset,
    URIType,
    pass_context,
    PPLResultStorage,
    PPLResultIterator,
    multi_classification,
)
from starwhale.api.service import api

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class CustomPipelineHandler:
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)
        self.batch_size = 5

    @step(concurrency=2, task_num=2)
    @pass_context
    def run_ppl(self, context: Context) -> None:
        print(f"start to run ppl@{context.version}-{context.total}-{context.index}...")
        ppl_result_storage = PPLResultStorage(context)

        for uri_str in context.dataset_uris:
            _uri = URI(uri_str, expected_type=URIType.DATASET)
            ds = dataset(_uri)
            ds.make_distributed_consumption(session_id=context.version)
            for rows in ds.batch_iter(self.batch_size):
                try:
                    pred_values, probability_matrixs = self.batch_ppl(
                        [r[1] for r in rows]
                    )
                    for (
                        (_idx, _data, _annotations),
                        pred_value,
                        probability_matrix,
                    ) in zip(rows, pred_values, probability_matrixs):
                        _unique_id = f"{_uri.object}_{_idx}"
                        ppl_result_storage.save(
                            data_id=_unique_id,
                            result=pred_value,
                            probability_matrix=probability_matrix,
                            annotations=_annotations,
                        )
                except Exception:
                    logger.error(f"[{[r[0] for r in rows]}] data handle -> failed")
                    raise

    @step(needs=["run_ppl"])
    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    @pass_context
    def run_cmp(
        self, context: Context
    ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
        print(f"start to run cmp@{context.version}...")
        result_loader = PPLResultIterator(context)
        result, label, pr = [], [], []
        for data in result_loader:
            result.append(data["result"])
            label.append(data["annotations"]["label"])
            pr.append(data["probability_matrix"])
        return label, result, pr

    def _load_model(self, device: torch.device) -> Net:
        model = Net().to(device)
        model.load_state_dict(
            torch.load(str(ROOTDIR / "models/mnist_cnn.pt"), map_location=device)  # type: ignore
        )
        model.eval()
        print("load mnist model, start to inference...")
        return model

    def _pre(self, data: t.List[Image]) -> t.Any:
        images = []
        for i in data:
            _tensor = torch.tensor(bytearray(i.to_bytes()), dtype=torch.uint8).reshape(
                i.shape[0], i.shape[1]  # type: ignore
            )
            _image_array = PILImage.fromarray(_tensor.numpy())
            _image = transforms.Compose(
                [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
            )(_image_array)

            images.append(_image)
        return torch.stack(images).to(self.device)

    def ppl(
        self, data: Image, **kw: t.Any
    ) -> t.Tuple[t.List[int], t.List[t.List[float]]]:
        return self.batch_ppl([data])

    def batch_ppl(
        self, images: t.List[Image], **kw: t.Any
    ) -> t.Tuple[t.List[int], t.List[t.List[float]]]:
        data_tensor = self._pre(images)
        output = self.model(data_tensor)
        return output.argmax(1).flatten().tolist(), np.exp(output.tolist()).tolist()

    @api(gradio.Sketchpad(shape=(28, 28), image_mode="L"), gradio.Label())
    def draw(self, data: np.ndarray) -> t.Any:
        _image_array = PILImage.fromarray(data.astype("int8"), mode="L")
        _image = transforms.Compose(
            [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
        )(_image_array)
        output = self.model(torch.stack([_image]).to(self.device))
        return {i: p for i, p in enumerate(np.exp(output.tolist()).tolist()[0])}

    @api(gradio.File(), gradio.Label())
    def upload_bin_file(self, file: t.Any) -> t.Any:
        with open(file.name, "rb") as f:
            data = Image(f.read(), shape=(28, 28, 1))
        _, prob = self.ppl(data)
        return {i: p for i, p in enumerate(prob[0])}
