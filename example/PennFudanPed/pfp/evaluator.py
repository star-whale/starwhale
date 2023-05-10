import io
import os
import random

import torch
import gradio
from PIL import Image as PILImage
from PIL import ImageDraw
from pycocotools.coco import COCO
from torchvision.transforms import functional

from starwhale import Image, MIMEType, evaluation
from starwhale.api.service import api

from .model import pretrained_model
from .utils import get_model_path
from .utils.coco_eval import CocoEvaluator

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
IOU_TYPES = ["bbox", "segm"]
mask_rcnn_global = None


def get_mask_rcnn_model():
    global mask_rcnn_global
    if mask_rcnn_global is not None:
        return mask_rcnn_global

    net = pretrained_model(
        2,
        model_local_dict=torch.load(get_model_path(), map_location=DEVICE),
    )
    net = net.to(DEVICE)
    net.eval()
    print("mask rcnn model loaded, start to inference...")
    mask_rcnn_global = net
    return net


@torch.no_grad()
@evaluation.predict(resources={"nvidia.com/gpu": 1})
def predict_mask_rcnn(data, external):
    index = external["index"]
    if isinstance(index, str) and "_" in index and index.startswith("dataset-"):
        # v0.3.2 SDK index contains dataset name-version prefix, such as: 'dataset-pfp-small-d2zbajpbvotc7g7qwbev7lhqwvvu4k33qj5pehkf_PNGImages/FudanPed00001.png'
        # other versions index is the origin data row index
        index = index.split("_")[-1]

    _img = PILImage.open(io.BytesIO(data["image"].to_bytes())).convert("RGB")
    _tensor = functional.to_tensor(_img).to(DEVICE)
    output = get_mask_rcnn_model()(torch.stack([_tensor]))

    ret = {}
    pred = {k: v.cpu() for k, v in output[0].items()}
    for typ in IOU_TYPES:
        ret[typ] = CocoEvaluator.prepare_predictions({index: pred}, typ)
    return index, ret


@evaluation.evaluate(needs=[predict_mask_rcnn])
def cmp(ppl_result):
    pred_results, annotations = [], []
    for _data in ppl_result:
        annotations.append(_data["input"])
        pred_results.append(_data["output"])

    evaluator = make_coco_evaluator(annotations, iou_types=IOU_TYPES)
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

    evaluation.log_summary(report)


@api(
    gradio.Image(type="filepath"),
    [gradio.Image(type="pil"), gradio.Json()],
    examples=[[os.path.join(os.path.dirname(__file__), "../FudanPed00001.png")]],
)
def handler(file: str):
    with open(file, "rb") as f:
        data = f.read()
    img = Image(data, mime_type=MIMEType.PNG)
    _, res = predict_mask_rcnn({"image": img}, 0)

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


def make_coco_evaluator(ann_list, iou_types) -> CocoEvaluator:
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
