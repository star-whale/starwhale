import io
import os
import random
import typing as t

import torch
import gradio
from PIL import Image as PILImage
from PIL import ImageDraw
from pycocotools.coco import COCO
from torchvision.transforms import functional

from starwhale import Image, PipelineHandler
from starwhale.api.service import api
from starwhale.core.dataset.type import MIMEType

from .model import pretrained_model
from .utils import get_model_path
from .utils.coco_eval import CocoEvaluator


class MaskRCnn(PipelineHandler):
    def __init__(self) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)
        self.iou_types = ["bbox", "segm"]
        super().__init__()

    @torch.no_grad()
    def ppl(self, data: dict, index: t.Union[int, str], **kw):
        if isinstance(index, str) and "_" in index and index.startswith("dataset-"):
            # v0.3.2 SDK index contains dataset name-version prefix, such as: 'dataset-pfp-small-d2zbajpbvotc7g7qwbev7lhqwvvu4k33qj5pehkf_PNGImages/FudanPed00001.png'
            # other versions index is the origin data row index
            index = index.split("_")[-1]

        _img = PILImage.open(io.BytesIO(data["image"].to_bytes())).convert("RGB")
        _tensor = functional.to_tensor(_img).to(self.device)
        output = self.model(torch.stack([_tensor]))
        return index, self._post(index, output[0])

    @api(
        gradio.Image(type="filepath"),
        [gradio.Image(type="pil"), gradio.Json()],
        examples=[[os.path.join(os.path.dirname(__file__), "../FudanPed00001.png")]],
    )
    def handler(self, file: str):
        with open(file, "rb") as f:
            data = f.read()
        img = Image(data, mime_type=MIMEType.PNG)
        _, res = self.ppl({"image": img}, 0)

        bbox = res["bbox"]
        _img = PILImage.open(file)
        draw = ImageDraw.ImageDraw(_img)
        for box in bbox:
            x, y, w, h = box["bbox"]
            color = (
                random.randint(0, 255),
                random.randint(0, 255),
                random.randint(0, 255),
            )
            draw.rectangle((x, y, x + w, y + h), outline=color)
        return _img, bbox

    def _post(
        self, index: t.Union[int, str], pred: t.Dict[str, torch.Tensor]
    ) -> t.Dict[str, t.Any]:
        output = {}
        pred = {k: v.cpu() for k, v in pred.items()}
        for typ in self.iou_types:
            output[typ] = CocoEvaluator.prepare_predictions({index: pred}, typ)
        return output

    def cmp(self, ppl_result):
        pred_results, annotations = [], []
        for _data in ppl_result:
            annotations.append(_data["ds_data"])
            pred_results.append(_data["result"])

        evaluator = make_coco_evaluator(annotations, iou_types=self.iou_types)
        for index, pred in pred_results:
            evaluator.update({index: pred})

        evaluator.synchronize_between_processes()
        evaluator.accumulate()
        evaluator.summarize()

        detector_metrics_map = [
            "average_precision",
            "average_precision_iou50",
            "average_precision_iou75",
            "ap_across_scales_small",
            "ap_across_scales_medium",
            "ap_across_scales_large",
            "average_recall_max1",
            "average_recall_max10",
            "average_recall_max100",
            "ar_across_scales_small",
            "ar_across_scales_medium",
            "ar_across_scales_large",
        ]

        report = {"kind": "coco_object_detection", "bbox": {}, "segm": {}}
        for _iou, _eval in evaluator.coco_eval.items():
            if _iou not in report:
                continue

            _stats = _eval.stats.tolist()
            for _idx, _label in enumerate(detector_metrics_map):
                report[_iou][_label] = _stats[_idx]

        self.evaluation_store.log_metrics(report)

    def _load_model(self, device):
        net = pretrained_model(
            2,
            model_local_dict=torch.load(get_model_path(), map_location=device),
        )
        net = net.to(device)
        net.eval()
        print("mask rcnn model loaded, start to inference...")
        return net


def make_coco_evaluator(
    ann_list: t.List[t.Dict], iou_types: t.List[str]
) -> CocoEvaluator:
    images = []
    categories = set()
    annotations = []
    for _anno in ann_list:
        images.append(
            {
                "id": _anno["image_id"],
                "height": _anno["image_height"],
                "width": _anno["image_width"],
                "name": _anno["image_name"],
            }
        )
        for _a in _anno["annotations"]:
            categories.add(_a.category_id)
            annotations.append(
                {
                    "id": _a.id,
                    "image_id": _a.image_id,
                    "category_id": _a.category_id,
                    "area": _a.area,
                    "bbox": _a.bbox,
                    "iscrowd": _a.iscrowd,
                    "segmentation": _a.segmentation,
                }
            )

    coco = COCO()
    coco.dataset = {
        "images": images,
        "annotations": annotations,
        "categories": [{"id": _c} for _c in sorted(categories)],
    }
    coco.createIndex()
    coco_evaluator = CocoEvaluator(coco, iou_types=iou_types)
    return coco_evaluator
