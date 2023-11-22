from __future__ import annotations

import io
import typing as t
from pathlib import Path
from collections import defaultdict

import numpy as np
import torch
import gradio
from PIL import Image as PILImage
from pycocotools import mask as mask_utils
from segment_anything import sam_model_registry, SamAutomaticMaskGenerator
from segment_anything.utils.amg import rle_to_mask, coco_encode_rle

from starwhale import Image, evaluation, Evaluation
from starwhale.api.service import api
from starwhale.base.data_type import COCOObjectAnnotation

MODEL_TYPE = "vit_b"
MODEL_CHECKPOINT_PATH = Path(__file__).parent / "checkpoints" / "sam_vit_b_01ec64.pth"
g_generator = None


def _load_sam_generator() -> t.Any:
    global g_generator
    if g_generator is None:
        print("load sam model...")
        sam = sam_model_registry[MODEL_TYPE](checkpoint=MODEL_CHECKPOINT_PATH)
        if torch.cuda.is_available():
            sam.to(device="cuda")
        # We will handle segmentation for coco-rle and binary-mask types by manually.
        g_generator = SamAutomaticMaskGenerator(sam, output_mode="uncompressed_rle")

    return g_generator


# By default, the predict task will run on GPU with 2 replicas. You can modify the arguments in the Server Web UI.
@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
    log_dataset_features=[],  # do not log dataset features into results table
)
def mask_image(data: t.Dict, external: t.Dict) -> t.Any:
    # current only supports coco2017-stuff dataset.
    masks = generate_mask(data["image"])
    mask_image = draw_masks_on_original_image(masks, data["image"])

    # get dataset row id from external argument
    row_id = external["index"]
    # get Evaluation instance from the current context
    e = Evaluation.from_context()
    e.log(
        category="masks",
        id=row_id,
        metrics={
            "input": {
                "image": data["image"],
                "pixelmaps": data["pixelmaps"],
                "masks_cnt": len(data["annotations"]),
            },
            "output": {"masks_cnt": len(masks), "mask_image": mask_image},
        },
    )

    # Starwhale.predict will automatically save the output to the results table,
    # then you can iter them in the evaluation function.
    return calculate_segmentation_metrics(data["annotations"], masks)


@evaluation.evaluate(needs=[mask_image])
def summary(predict_result_iter: t.Iterator) -> None:
    metrics = defaultdict(list)
    for predict in predict_result_iter:
        for k, v in predict.items():
            if not k.startswith("output/"):
                continue

            metrics[k].append(v)

    e = Evaluation.from_context()
    e.log_summary({k: round(np.mean(v), 4) for k, v in metrics.items()})


def draw_masks_on_original_image(masks: t.List, original: Image) -> Image:
    sorted_anns = sorted(masks, key=(lambda x: x["area"]), reverse=True)
    mask_img = np.ones(
        (
            sorted_anns[0]["segmentation_binary_mask"].shape[0],
            sorted_anns[0]["segmentation_binary_mask"].shape[1],
            3,
        )
    )
    for ann in sorted_anns:
        mask_img[ann["segmentation_binary_mask"]] = np.random.random(3)
    alpha = 0.35
    img = mask_img * alpha + (original.to_numpy("RGB").astype(float) / 255) * (
        1 - alpha
    )

    img = PILImage.fromarray((img * 255).astype(np.uint8))
    img_byte_array = io.BytesIO()
    img.save(img_byte_array, format="PNG")
    return Image(img_byte_array.getvalue())


def generate_mask(img: Image) -> t.List[t.Dict]:
    generator = _load_sam_generator()
    masks = generator.generate(img.to_numpy("RGB"))

    for idx in range(len(masks)):
        # add "segmentation_coco_rle" and "segmentation_binary_mask" to the output.
        s = masks[idx]["segmentation"]
        # refer: https://github.com/facebookresearch/segment-anything/blob/main/segment_anything/automatic_mask_generator.py#L174
        masks[idx]["segmentation_coco_rle"] = coco_encode_rle(s)
        masks[idx]["segmentation_binary_mask"] = rle_to_mask(s)
    return masks


@api(
    gradio.Image(type="filepath", label="Input Image"),
    [gradio.Image(type="numpy", label="Masked Image"), gradio.JSON(label="Masks Info")],
)
def generate_mask_view(file: t.Any) -> t.Any:
    with open(file, "rb") as f:
        img = Image(f.read())

    masks = generate_mask(img)
    mask_img = draw_masks_on_original_image(masks, img)
    for m in masks:
        del m["segmentation"]
        del m["segmentation_binary_mask"]
        del m["segmentation_coco_rle"]
    return mask_img.to_numpy(), masks


def calculate_segmentation_metrics(
    ground_truth: t.List[COCOObjectAnnotation], predictions: t.List[t.Dict]
) -> t.Dict:
    bbox_metrics_array = defaultdict(list)
    mask_metrics_array = defaultdict(list)

    for pred in predictions:
        p_mask = mask_utils.decode(pred["segmentation_coco_rle"])
        _max_metrics = defaultdict(float)

        for gt in ground_truth:
            # calculate IoU and Dice by bbox
            _iou, _dice = calculate_bbox_iou_and_dice(gt.bbox, pred["bbox"])
            _max_metrics["bbox/iou"] = max(_max_metrics["bbox/iou"], _iou)
            _max_metrics["bbox/dice"] = max(_max_metrics["bbox/dice"], _dice)

            # calculate IoU, Dice, precision, recall and accuracy by mask
            gt_mask = mask_utils.decode(gt.segmentation)
            _max_metrics["iou"] = max(
                _max_metrics["iou"], calculate_mask_iou(p_mask, gt_mask)
            )
            _max_metrics["dice"] = max(
                _max_metrics["dice"], calculate_mask_dice(p_mask, gt_mask)
            )
            _max_metrics["pixel_accuracy"] = max(
                _max_metrics["pixel_accuracy"],
                calculate_mask_pixel_accuracy(p_mask, gt_mask),
            )
            _max_metrics["precision"] = max(
                _max_metrics["precision"], calculate_mask_precision(p_mask, gt_mask)
            )
            _max_metrics["recall"] = max(
                _max_metrics["recall"], calculate_mask_recall(p_mask, gt_mask)
            )

        for k, v in _max_metrics.items():
            if k.startswith("bbox/"):
                bbox_metrics_array[k.split("/")[1]].append(v)
            else:
                mask_metrics_array[k].append(v)

    return {
        "bbox": {k: round(np.mean(v), 4) for k, v in bbox_metrics_array.items()},
        "mask": {k: round(np.mean(v), 4) for k, v in mask_metrics_array.items()},
    }


def calculate_mask_iou(
    predict_mask: np.ndarray, ground_truth_mask: np.ndarray
) -> float:
    # iou = TP / (TP + FP + FN)
    intersection = np.logical_and(predict_mask, ground_truth_mask).sum()
    union = np.logical_or(predict_mask, ground_truth_mask).sum()
    return intersection / union


def calculate_mask_dice(
    predict_mask: np.ndarray, ground_truth_mask: np.ndarray
) -> float:
    # dice = 2 * TP / (2TP + FP + FN)
    intersection = np.logical_and(predict_mask, ground_truth_mask).sum()
    return 2 * intersection / (predict_mask.sum() + ground_truth_mask.sum())


def calculate_mask_pixel_accuracy(
    predict_mask: np.ndarray, ground_truth_mask: np.ndarray
) -> float:
    # accuracy = (TP + TN) / (TP + TN + FP + FN)
    intersection = np.logical_and(predict_mask, ground_truth_mask).sum()
    union = np.logical_or(predict_mask, ground_truth_mask).sum()
    equal = (predict_mask == ground_truth_mask).sum()
    return equal / (union + equal - intersection)


def calculate_mask_precision(
    predict_mask: np.ndarray, ground_truth_mask: np.ndarray
) -> float:
    # precision = TP / (TP + FP)
    correct = np.logical_and(predict_mask, ground_truth_mask).sum()
    return correct / predict_mask.sum()


def calculate_mask_recall(
    predict_mask: np.ndarray, ground_truth_mask: np.ndarray
) -> float:
    # recall = TP / (TP + FN)
    correct = np.logical_and(predict_mask, ground_truth_mask).sum()
    return correct / ground_truth_mask.sum()


def calculate_bbox_iou_and_dice(b1: t.List, b2: t.List) -> t.Tuple[float, float]:
    x1, y1, w1, h1 = b1
    x2, y2, w2, h2 = b2

    x_intersection = max(0, min(x1 + w1, x2 + w2) - max(x1, x2))
    y_intersection = max(0, min(y1 + h1, y2 + h2) - max(y1, y2))
    intersection = x_intersection * y_intersection
    union = w1 * h1 + w2 * h2 - intersection

    iou = intersection / union
    dice = 2 * intersection / (w1 * h1 + w2 * h2)
    return iou, dice
