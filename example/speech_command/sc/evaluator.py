import io
import typing as t
from pathlib import Path

import numpy as np
import torch
import torchaudio

from starwhale import Audio, Context, PipelineHandler, multi_classification

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


class M5Inference(PipelineHandler):
    def __init__(self, context: Context) -> None:
        super().__init__(context=context, ignore_error=False)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = self._load_model(self.device)
        self.transform = torchaudio.transforms.Resample(
            orig_freq=16000, new_freq=8000
        ).to(self.device)

    @torch.no_grad()
    def ppl(self, audio: Audio, **kw):
        _tensor = self._pre(audio)
        output = self.model(_tensor)
        return self._post(output)

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, len(ALL_LABELS))],
    )
    def cmp(self, ppl_result):
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(ALL_LABELS_MAP[_data["annotations"]["label"]])
            pr.append(_data["result"][1])
            result.append(_data["result"][0][0])
        return label, result, pr

    def _pre(self, input: Audio) -> torch.Tensor:
        _audio = io.BytesIO(input.to_bytes())
        waveform, _ = torchaudio.load(_audio)
        waveform = torch.nn.utils.rnn.pad_sequence(
            [waveform.t()], batch_first=True, padding_value=0.0
        )
        waveform = waveform.permute(0, 2, 1)
        return self.transform(waveform.to(self.device))

    def _post(self, input: torch.Tensor) -> t.Tuple[t.List[int], t.List[float]]:
        input = input.squeeze()
        pred_value = input.argmax(-1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix

    def _load_model(self, device):
        model = M5(n_input=1, n_output=len(ALL_LABELS))
        model.load_state_dict(torch.load(str(ROOTDIR / "models/m5.pth"), map_location=device))
        model.to(device)
        model.eval()
        print("m5 model loaded, start to inference...")
        return model
