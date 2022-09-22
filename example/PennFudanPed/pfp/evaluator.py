import io
import typing as t

import torch
from PIL import Image as PILImage
from pycocotools.coco import COCO
from torchvision.transforms import functional

from starwhale import Image, Context, PipelineHandler

from .model import pretrained_model
from .utils import get_model_path
from .utils.coco_eval import CocoEvaluator


class MaskRCnn(PipelineHandler):
    def __init__(self, context: Context) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)
        self.iou_types = ["bbox", "segm"]
        super().__init__(context=context)

    @torch.no_grad()
    def ppl(self, img: Image, index: int, **kw):
        _img = PILImage.open(io.BytesIO(img.to_bytes())).convert("RGB")
        _tensor = functional.to_tensor(_img).to(self.device)
        output = self.model(torch.stack([_tensor]))
        return index, self._post(index, output[0])

    def _post(self, index: int, pred: t.Dict[str, torch.Tensor]) -> t.Dict[str, t.Any]:
        output = {}
        pred = {k: v.cpu() for k, v in pred.items()}
        for typ in self.iou_types:
            output[typ] = CocoEvaluator.prepare_predictions({index: pred}, typ)
        return output

    def cmp(self, ppl_result):
        pred_results, annotations = [], []
        for _data in ppl_result:
            annotations.append(_data["annotations"])
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

        self.evaluation.log_metrics(report)

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
        images.append(_anno["image"])
        for _a in _anno["annotations"]:
            categories.add(_a["category_id"])
            annotations.append(_a)

    coco = COCO()
    coco.dataset = {
        "images": images,
        "annotations": annotations,
        "categories": [{"id": _c} for _c in sorted(categories)],
    }
    coco.createIndex()
    coco_evaluator = CocoEvaluator(coco, iou_types=iou_types)
    return coco_evaluator
