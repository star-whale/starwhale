from pathlib import Path
from dataclasses import field, dataclass

import numpy as np
import onnxruntime as rt

from starwhale import Image, argument, evaluation, multi_classification

_g_model = None


@dataclass
class EvaluationArguments:
    reshape: int = field(default=64, metadata={"help": "reshape image size"})


def _load_model():
    global _g_model

    if _g_model is None:
        _g_model = rt.InferenceSession(
            str(Path(__file__).parent / "knn.onnx"), providers=["CPUExecutionProvider"]
        )

    return _g_model


@argument(EvaluationArguments)
@evaluation.predict(
    resources={"memory": {"request": "500M", "limit": "2G"}},
    log_mode="plain",
)
def predict_image(data: dict, argument: EvaluationArguments):
    # def predict_image(data: dict, argument: EvaluationArguments):
    # def predict_image(data: dict, external=None):
    # def predict_image(data: dict, external=None, argument: EvaluationArguments=None):
    img: Image = data["img"]
    model = _load_model()
    input_name = model.get_inputs()[0].name
    label_name = model.get_outputs()[0].name
    input_array = [img.to_numpy().astype(np.float32).reshape(argument.reshape)]
    pred = model.run([label_name], {input_name: input_array})[0]
    return pred.item()


@evaluation.evaluate(
    needs=[predict_image],
    resources={"memory": {"request": "500M", "limit": "2G"}},
)
@multi_classification(
    confusion_matrix_normalize="all",
    show_hamming_loss=True,
    show_cohen_kappa_score=True,
    show_roc_auc=False,
    all_labels=[i for i in range(0, 10)],
)
def evaluate_results(predict_result_iter):
    result, label = [], []
    for _data in predict_result_iter:
        label.append(_data["input"]["label"])
        result.append(_data["output"])
    return label, result
