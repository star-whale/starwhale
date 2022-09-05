import io
import pickle
from pathlib import Path

import numpy as np
import torch
import torchaudio

from starwhale.api.job import Context
from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

from .model import M5

ROOTDIR = Path(__file__).parent.parent
labels = [
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
    "ERROR",
]


class M5Inference(PipelineHandler):
    def __init__(self, context: Context) -> None:
        super().__init__(context=context)
        self.device = torch.device("cpu")
        self.model = self._load_model(self.device)
        self.transform = torchaudio.transforms.Resample(orig_freq=16000, new_freq=8000)
        self.transform = self.transform.to("cpu")

    def ppl(self, data, **kw):
        audios = self._pre(data)
        result = []
        for audio_f in audios:
            try:
                label_idx = self.model(audio_f.unsqueeze(0)).argmax(dim=-1).squeeze()
                result.append(labels[label_idx])
            except Exception:
                result.append("ERROR")
        return result

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=False,
        all_labels=labels,
    )
    def cmp(self, ppl_result):
        result, label = [], []
        for _data in ppl_result:
            label.append(_data["annotations"]["label"])
            (result) = _data["result"]
            result.extend(result)
        return result, label

    def _pre(self, input: bytes):
        audios = pickle.loads(input)
        _result = []
        for file_bytes in audios:
            # you could debug the file name by watching file_bytes.file_path
            bytes_io = io.BytesIO(file_bytes.content_bytes)
            test_tensor = torchaudio.load(bytes_io)[0].to(self.device)
            test_tensor = self.transform(test_tensor)
            _result.append(test_tensor.to(self.device))
        return _result

    def _post(self, input):
        pred_value = input.argmax(1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix

    def _load_model(self, device):
        model = M5(n_input=1, n_output=35)
        model.load_state_dict(torch.load(str(ROOTDIR / "models/m5.pth")))
        model.to(device)
        model.eval()
        print("m5 model loaded, start to inference...")
        return model
