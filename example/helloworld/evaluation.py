from pathlib import Path

import numpy as np
import onnxruntime as rt

from starwhale import Image, evaluation, multi_classification

_g_model = None


def _load_model():
    global _g_model

    if _g_model is None:
        _g_model = rt.InferenceSession(
            str(Path(__file__).parent / "knn.onnx"), providers=["CPUExecutionProvider"]
        )

    return _g_model


@evaluation.predict(
    resources={"memory": {"request": "500M", "limit": "2G"}},
    log_mode="plain",
)
def predict_image(data):
    img: Image = data["img"]
    model = _load_model()
    input_name = model.get_inputs()[0].name
    label_name = model.get_outputs()[0].name
    input_array = [img.to_numpy().astype(np.float32).reshape(64)]
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
