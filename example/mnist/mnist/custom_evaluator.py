import typing as t
from types import TracebackType
from pathlib import Path

import numpy as np
import torch
from PIL import Image as PILImage
from loguru import logger
from torchvision import transforms

from starwhale import (
    step,
    Image,
    Context,
    pass_context,
    PPLResultStorage,
    PPLResultIterator,
    multi_classification,
    get_sharding_data_loader,
)

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class CustomPipelineHandler:
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

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

    @step(concurrency=2, task_num=2)
    @pass_context
    def run_ppl(self, context: Context) -> None:
        print(f"start to run ppl@{context.version}-{context.total}-{context.index}...")
        ppl_result_storage = PPLResultStorage(context)

        _data_loader = get_sharding_data_loader(
            dataset_uri=context.dataset_uris[0],
            sharding_index=context.index,
            sharding_num=context.total,
        )

        for _idx, _data, _annotations in _data_loader:
            try:
                data_tensor = self._pre(_data)
                output = self.model(data_tensor)

                pred_value = output.argmax(1).flatten().tolist()
                probability_matrix = np.exp(output.tolist()).tolist()

                ppl_result_storage.save(
                    data_id=_idx,
                    result=pred_value,
                    probability_matrix=probability_matrix,
                    annotations=_annotations,
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
    @pass_context
    def run_cmp(
        self, context: Context
    ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
        print(f"start to run cmp@{context.version}...")
        result_loader = PPLResultIterator(context)
        result, label, pr = [], [], []
        for data in result_loader:
            result.extend(data["result"])
            label.append(data["annotations"]["label"])
            pr.extend(data["probability_matrix"])
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
