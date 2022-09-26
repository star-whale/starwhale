import math
import typing as t
from types import TracebackType
from pathlib import Path

import dill
import numpy as np
import torch
from PIL import Image as PILImage
from loguru import logger
from torchvision import transforms

from starwhale import (
    step,
    Image,
    Context,
    Evaluation,
    get_data_loader,
    multi_classification,
)
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.core.dataset.model import Dataset

from .model import Net

ROOTDIR = Path(__file__).parent.parent


def calculate_index(
    data_size: int, task_num: int, task_index: int
) -> t.Tuple[int, int]:
    _batch_size = 1
    if data_size > task_num:
        _batch_size = math.ceil(data_size / task_num)
    _start_index = min(_batch_size * task_index, data_size - 1)
    _end_index = min(_batch_size * (task_index + 1) - 1, data_size - 1)
    return _start_index, _end_index


def _serialize(data: t.Any) -> t.Any:
    return dill.dumps(data)


def _deserialize(data: bytes) -> t.Any:
    return dill.loads(data)


class CustomPipelineHandler:
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)
        self.evaluation = t.Optional[None]

    def __enter__(self) -> "CustomPipelineHandler":
        print("enter custom handler!")
        return self

    def __exit__(
        self,
        exc_type: t.Optional[t.Type[BaseException]],
        exc_val: t.Optional[BaseException],
        exc_tb: TracebackType,
    ) -> None:
        if exc_val:
            print(f"type:{exc_type}, exception:{exc_val}, traceback:{exc_tb}")
        print("exit custom handler!")

    @step()
    def run_ppl(self, context: Context) -> None:
        print(f"start to run ppl@{context.version}...")
        if not context.dataset_uris:
            raise RuntimeError("context.dataset_uris is empty")
        self.evaluation = Evaluation(eval_id=context.version, project=context.project)
        _dataset_uri = URI(context.dataset_uris[0], expected_type=URIType.DATASET)
        _dataset = Dataset.get_dataset(_dataset_uri)
        _dataset_summary = _dataset.summary()
        _dataset_rows = _dataset_summary.rows if _dataset_summary else 0
        dataset_row_start, dataset_row_end = calculate_index(
            _dataset_rows, context.total, context.index
        )

        _data_loader = get_data_loader(
            dataset_uri=_dataset_uri,
            start=dataset_row_start,
            end=dataset_row_end + 1,
        )
        for _idx, img, _annotations in _data_loader:
            try:
                data_tensor = self._pre(img)
                output = self.model(data_tensor)
                res = self._post(output)
                self.evaluation.log_result(
                    data_id=_idx,
                    result=_serialize(res),
                    annotations=_serialize(_annotations),
                )
            except Exception:
                logger.error(f"[{_idx}] data handle -> failed")
                raise

    @step(needs=["run_ppl"])
    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def run_cmp(
        self, context: Context
    ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
        print(f"start to run cmp@{context.version}...")
        result, label, pr = [], [], []
        self.evaluation = Evaluation(eval_id=context.version, project=context.project)
        for data in self.evaluation.get_results():
            ppl_res = _deserialize(data["result"])
            annotations = _deserialize(data["annotations"])
            label.append(annotations["label"])
            result.extend(ppl_res[0])
            pr.extend(ppl_res[1])
        return label, result, pr

    def _load_model(self, device: torch.device) -> Net:
        model = Net().to(device)
        model.load_state_dict(
            torch.load(str(ROOTDIR / "models/mnist_cnn.pt"), map_location=device)  # type: ignore
        )
        model.eval()
        print("load mnist model, start to inference...")
        return model

    def _pre(self, input: Image) -> torch.Tensor:
        _tensor = torch.tensor(bytearray(input.to_bytes()), dtype=torch.uint8).reshape(
            input.shape[0], input.shape[1]  # type: ignore
        )
        _image_array = PILImage.fromarray(_tensor.numpy())
        _image = transforms.Compose(
            [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
        )(_image_array)
        return torch.stack([_image]).to(self.device)

    def _post(self, input: torch.Tensor) -> t.Tuple[t.List[int], t.List[float]]:
        pred_value = input.argmax(1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix
