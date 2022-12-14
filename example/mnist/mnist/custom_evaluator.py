import json
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
    dataset,
    URIType,
    pass_context,
    GrayscaleImage,
    PPLResultStorage,
    PPLResultIterator,
    multi_classification,
)
from starwhale.api.service import Input, Output, Request, Service, Response
from starwhale.base.spec.openapi.components import (
    Schema,
    MediaType,
    RequestBody,
    SpecComponent,
    OpenApiResponse,
)

from .model import Net

ROOTDIR = Path(__file__).parent.parent


class CustomService(Service):
    def __init__(self) -> None:
        super().__init__()

    def serve(
        self, addr: str, port: int, handler_list: t.Optional[t.List[str]] = None
    ) -> None:
        print(f"My CustomService running {self.apis}")
        super().serve(addr, port, handler_list)


svc = CustomService()


class CustomGrayscaleImageRequest(Input):
    def load(self, req: Request) -> GrayscaleImage:
        raw = req.body
        return GrayscaleImage(raw, shape=[28, 28, 3])

    def spec(self) -> SpecComponent:
        req = RequestBody(
            description="starwhale mnist custom grayscale image request",
            content={
                "multipart/form-data": MediaType(
                    schema=Schema(
                        type="object",
                        required=["data"],
                        properties={"data": Schema(type="string", format="binary")},
                    ),
                )
            },
        )
        return SpecComponent(requestBody=req)


class CustomOutput(Output):
    def dump(self, *args: t.Any) -> Response:
        return Response(
            json.dumps(
                {
                    "result": args[0][0][0],
                    "probabilities": args[0][1][0],
                }
            ).encode("utf-8"),
            {"content-type": "application/json"},
        )

    def spec(self) -> SpecComponent:
        resp = OpenApiResponse(
            description="OK",
            content={
                "application/json": MediaType(
                    schema=Schema(
                        type="object",
                        properties={
                            "result": Schema(type="integer"),
                            "probabilities": Schema(
                                type="array", items=Schema(type="number")
                            ),
                        },
                    ),
                )
            },
        )
        return SpecComponent(responses={"200": resp})


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

    @svc.api(CustomGrayscaleImageRequest(), CustomOutput())
    def ppl(self, data: Image, **kw: t.Any) -> t.Tuple[t.List[int], t.List[float]]:
        return self.batch_ppl([data])

    def batch_ppl(
        self, images: t.List[Image], **kw: t.Any
    ) -> t.Tuple[t.List[int], t.List[float]]:
        data_tensor = self._pre(images)
        output = self.model(data_tensor)
        return output.argmax(1).flatten().tolist(), np.exp(output.tolist()).tolist()

    @svc.api(
        CustomGrayscaleImageRequest(),
        CustomOutput(),
        uri="custom_handler",
    )
    def custom_svc_handler(self, data: t.Any) -> t.Any:
        return self.ppl(data)
