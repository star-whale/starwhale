from __future__ import annotations

import io
import typing as t
from pathlib import Path

import dill
import numpy as np
import torch
import gradio
from PIL import Image as PILImage
from segment_anything import sam_model_registry, SamAutomaticMaskGenerator
from segment_anything.utils.amg import rle_to_mask, coco_encode_rle

from starwhale import Image, evaluation, Evaluation
from starwhale.api.service import api

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
    auto_log=False,
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

    e.log(
        category="coco-rle",
        id=row_id,
        metrics={
            "ground_truth": dill.dumps(data["annotations"]),
            "prediction": dill.dumps([m["segmentation_coco_rle"] for m in masks]),
        },
    )


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
    img = mask_img * alpha + (
        np.array(original.to_pil().convert("RGB")).astype(float) / 255
    ) * (1 - alpha)
    img = PILImage.fromarray((img * 255).astype(np.uint8))
    img_byte_array = io.BytesIO()
    img.save(img_byte_array, format="PNG")
    return Image(img_byte_array.getvalue())


def generate_mask(img: Image) -> t.List[t.Dict]:
    generator = _load_sam_generator()
    # Starwhale.Image has `to_pil` method to convert to Pillow Image.
    # later we will support `to_numpy` method to convert to numpy array
    masks = generator.generate(np.array(img.to_pil().convert("RGB")))

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
    uri="/mask",
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
