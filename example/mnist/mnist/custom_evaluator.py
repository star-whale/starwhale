import typing as t
from pathlib import Path

import numpy as np
import torch
from PIL import Image as PILImage
from loguru import logger
from torchvision import transforms

from starwhale import (
    URI,
    step,
    Image,
    Context,
    URIType,
    pass_context,
    get_data_loader,
    PPLResultStorage,
    PPLResultIterator,
    multi_classification,
    get_dataset_consumption,
)

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class CustomPipelineHandler:
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)

    @step(concurrency=2, task_num=2)
    @pass_context
    def run_ppl(self, context: Context) -> None:
        print(f"start to run ppl@{context.version}-{context.total}-{context.index}...")
        ppl_result_storage = PPLResultStorage(context)

        for ds_uri in context.dataset_uris:
            _uri = URI(ds_uri, expected_type=URIType.DATASET)
            consumption = get_dataset_consumption(
                dataset_uri=_uri, session_id=context.version
            )
            loader = get_data_loader(_uri, session_consumption=consumption)

            for _idx, _data, _annotations in loader:
                _unique_id = f"{_uri.object}_{_idx}"
                try:
                    data_tensor = self._pre(_data)
                    output = self.model(data_tensor)

                    pred_value = output.argmax(1).flatten().tolist()
                    probability_matrix = np.exp(output.tolist()).tolist()

                    ppl_result_storage.save(
                        data_id=_unique_id,
                        result=pred_value,
                        probability_matrix=probability_matrix,
                        annotations=_annotations,
                    )
                except Exception:
                    logger.error(f"[{_unique_id}] data handle -> failed")
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
