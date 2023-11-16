from __future__ import annotations

import typing as t
from pathlib import Path

import gradio
from transformers import ViTImageProcessor, ViTForImageClassification

from starwhale import Image, evaluation, multi_classification
from starwhale.api.service import api

ROOT_DIR = Path(__file__).parent
MODELS_DIR = ROOT_DIR / "models"

g_processor = None
g_model = None


def _load_processor_and_model() -> t.Tuple:
    global g_model, g_processor
    if g_processor is None:
        g_processor = ViTImageProcessor.from_pretrained(MODELS_DIR)

    if g_model is None:
        g_model = ViTForImageClassification.from_pretrained(MODELS_DIR)

    return g_processor, g_model


def do_predict_image(img: Image) -> t.Dict:
    processor, model = _load_processor_and_model()
    inputs = processor(img.to_pil(), return_tensors="pt")
    logits = model(**inputs).logits
    idx = logits.argmax(-1).item()
    label = model.config.id2label[idx]
    return {
        "pred": idx,
        "pred_label": label,
        "prob": logits.tolist()[0],
    }


@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
    log_dataset_features=["img", "label", "label__classlabel__"],
)
def predict_image(data: t.Dict) -> t.Dict:
    # TODO: support CIFAR100 dataset
    return do_predict_image(data["img"])


@api(
    gradio.Image(type="filepath"),
    gradio.Json(title="Prediction"),
)
def predict_image_view(file: t.Any) -> t.Any:
    with open(file, "rb") as f:
        img = Image(f.read())
    return do_predict_image(img)


@multi_classification(
    confusion_matrix_normalize="all",
    show_hamming_loss=True,
    show_cohen_kappa_score=True,
    show_roc_auc=True,
    all_labels=[i for i in range(0, 10)],
)
@evaluation.evaluate(needs=[predict_image])
def reduce_evaluate(predict_result_iter: t.Iterator) -> t.Tuple:
    result, label, pr = [], [], []
    for _data in predict_result_iter:
        label.append(_data["input"]["label"])
        result.append(_data["output/pred"])
        pr.append(_data["output/prob"])
    return label, result, pr
