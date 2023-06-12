import io
from pathlib import Path

import numpy as np
import torch
import gradio
import torchaudio

from starwhale import Audio, evaluation, multi_classification
from starwhale.api.service import api

from .model import M5

ROOTDIR = Path(__file__).parent.parent
ALL_LABELS = sorted(
    [
        "backward",
        "bed",
        "bird",
        "cat",
        "dog",
        "down",
        "eight",
        "five",
        "follow",
        "forward",
        "four",
        "go",
        "happy",
        "house",
        "learn",
        "left",
        "marvin",
        "nine",
        "no",
        "off",
        "on",
        "one",
        "right",
        "seven",
        "sheila",
        "six",
        "stop",
        "three",
        "tree",
        "two",
        "up",
        "visual",
        "wow",
        "yes",
        "zero",
    ]
)

ALL_LABELS_MAP = {label: idx for idx, label in enumerate(ALL_LABELS)}
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
sample_transform = torchaudio.transforms.Resample(orig_freq=16000, new_freq=8000).to(
    DEVICE
)

m5_model_global = None


def get_m5_model():
    global m5_model_global
    if m5_model_global is not None:
        return m5_model_global

    model = M5(n_input=1, n_output=len(ALL_LABELS))
    model.load_state_dict(
        torch.load(str(ROOTDIR / "models/m5.pth"), map_location=DEVICE)
    )
    model.to(DEVICE)
    model.eval()
    print("m5 model loaded, start to inference...")
    m5_model_global = model
    return model


@torch.no_grad()
@evaluation.predict(resources={"nvidia.com/gpu": 1}, replicas=2)
def predict_speech(data):
    _audio = io.BytesIO(data["speech"].to_bytes())
    waveform, _ = torchaudio.load(_audio)
    waveform = torch.nn.utils.rnn.pad_sequence(
        [waveform.t()], batch_first=True, padding_value=0.0
    )
    waveform = waveform.permute(0, 2, 1)
    tensor = sample_transform(waveform.to(DEVICE))
    output = get_m5_model()(tensor)
    output = output.squeeze()
    pred_value = output.argmax(-1).item()
    probability_matrix = np.exp(output.tolist()).tolist()
    return pred_value, probability_matrix


@evaluation.evaluate(needs=[predict_speech])
@multi_classification(
    confusion_matrix_normalize="all",
    show_hamming_loss=True,
    show_cohen_kappa_score=True,
    show_roc_auc=True,
    all_labels=[i for i in range(0, len(ALL_LABELS))],
)
def evaluate_speech(ppl_result):
    result, label, pr = [], [], []
    for _data in ppl_result:
        label.append(ALL_LABELS_MAP[_data["input"]["command"]])
        pr.append(_data["output"][1])
        result.append(_data["output"][0])
    return label, result, pr


@api(
    gradio.Audio(type="filepath"),
    gradio.Label(),
)
def online_eval(file: str):
    with open(file, "rb") as f:
        data = f.read()
    _, prob = predict_speech({"speech": Audio(fp=data)})
    return {ALL_LABELS[i]: p for i, p in enumerate(prob)}
