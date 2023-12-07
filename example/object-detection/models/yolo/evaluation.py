from __future__ import annotations

import os
import pickle
import typing as t
from pathlib import Path

import torch
import gradio
from consts import COCO_CLASSES_MAP
from ultralytics import YOLO
from ultralytics.utils.metrics import DetMetrics
from ultralytics.models.yolo.detect.val import DetectionValidator

from starwhale import Image, evaluation, Evaluation
from starwhale.api.service import api

_g_model: YOLO | None = None

CHECKPOINT_DIR = Path(__file__).parent / "checkpoints"


def _load_model(is_evaluation: bool = False) -> YOLO:
    global _g_model
    if _g_model is None:
        # TODO: load model by build tag
        if (CHECKPOINT_DIR / ".model").exists():
            model_name = (CHECKPOINT_DIR / ".model").read_text().strip()
        else:
            model_name = "yolov8n"

        _g_model = YOLO(CHECKPOINT_DIR / f"{model_name}.pt")

    return _g_model


@torch.no_grad()
@evaluation.predict(replicas=1, log_mode="plain", log_dataset_features=["image"])
def predict_image(data: t.Dict, external: t.Dict) -> t.Dict:
    img = data["image"].to_pil()
    img.filename = f"{external['index']}.jpg"  # workaround for ultralytics save image

    # TODO: support batch
    # TODO: support arguments
    model = _load_model(is_evaluation=True)
    result = model.predict(
        img,
        save=True,
        conf=float(os.environ.get("OBJECT_DETECTION_CONF", "0.1")),
        iou=float(os.environ.get("OBJECT_DETECTION_IOU", "0.5")),
        max_det=int(os.environ.get("OBJECT_DETECTION_MAX_DET", "300")),
    )[0]
    device = model.device
    detection_validator = DetectionValidator()

    label_classes = torch.as_tensor(
        [[int(ann["class_id"])] for ann in data["annotations"]], device=device
    )

    label_bboxes_cnt = len(data["annotations"])
    pred_bboxes_cnt = result.boxes.data.shape[0]

    correct_bboxes = torch.zeros(
        pred_bboxes_cnt, detection_validator.niou, dtype=torch.bool, device=device
    )

    if pred_bboxes_cnt == 0:
        if label_bboxes_cnt:
            return {
                "speed": result.speed,
                "predicted_image": Image(os.path.join(result.save_dir, img.filename)),
                "ultralytics_metric": pickle.dumps(
                    (
                        correct_bboxes,
                        *torch.zeros((2, 0), device=device),
                        label_classes.squeeze(-1),
                    )
                ),
            }

    if label_bboxes_cnt:
        label_xyxy_bboxes = torch.as_tensor(
            [ann["bbox"].to_xyxy() for ann in data["annotations"]], device=device
        )

        correct_bboxes = detection_validator._process_batch(
            detections=result.boxes.data,
            labels=torch.cat((label_classes, label_xyxy_bboxes), dim=1),
        )

    # Copy from https://github.com/ultralytics/ultralytics/blob/main/ultralytics/models/yolo/detect/val.py#L89
    metric = (
        correct_bboxes,  # correct bboxes
        result.boxes.conf,  # predicted confidences
        result.boxes.cls,  # predicted classes
        label_classes.squeeze(-1),  # label classes
    )

    return {
        "speed": result.speed,
        "predicted_image": Image(os.path.join(result.save_dir, img.filename)),
        "ultralytics_metric": pickle.dumps(metric),
    }


@evaluation.evaluate(needs=[predict_image])
def summary_detection(predict_result_iter: t.Iterator) -> None:
    stats = []
    for predict in predict_result_iter:
        stats.append(pickle.loads(predict["output/ultralytics_metric"]))
    stats = [torch.cat(x, 0).cpu().numpy() for x in zip(*stats)]

    metrics = DetMetrics(names=COCO_CLASSES_MAP)
    metrics.process(*stats)

    e = Evaluation.from_context()
    # starwhale datastore is not support dict key with parentheses
    keys = [k.split("(B)")[0] for k in metrics.keys]
    e.log_summary(dict(zip(keys, metrics.mean_results())))

    for i, c in enumerate(metrics.ap_class_index):
        e.log(
            category="per_class",
            id=COCO_CLASSES_MAP[c],
            metrics=dict(zip(keys, metrics.class_result(i))),
        )


@torch.no_grad()
@api(
    gradio.Image(type="filepath", label="Input Image"),
    gradio.Image(type="filepath", label="Detected Image"),
)
def web_detect_image(file: str) -> Path | str:
    result = _load_model().predict(file, save=True)[0]
    return Path(result.save_dir) / Path(result.path).name
