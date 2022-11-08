import typing as t
import logging
import tempfile
from pathlib import Path

import cv2
import numpy as np
import torch

from starwhale import Video, PipelineHandler, PPLResultIterator, multi_classification

from .model import MFNET_3D
from .sampler import RandomSampling
from .transform import Resize, Compose, ToTensor, Normalize, RandomCrop

root_dir = Path(__file__).parent.parent


def ppl_post(output: torch.Tensor) -> t.Tuple[t.List[str], t.List[float]]:
    output = output.squeeze()
    pred_value = output.argmax(-1).flatten().tolist()
    probability_matrix = np.exp(output.tolist()).tolist()
    return [str(p) for p in pred_value], probability_matrix


def load_model(device):
    model = MFNET_3D(num_classes=101)
    # network
    if torch.cuda.is_available():
        model = torch.nn.DataParallel(model).cuda()
    else:
        model = torch.nn.DataParallel(model)

    checkpoint = torch.load(
        str(root_dir / "models/PyTorch-MFNet_ep-0000.pth"), map_location=device
    )

    # customized partially load function
    net_state_keys = list(model.state_dict().keys())
    for name, param in checkpoint["state_dict"].items():
        if name in model.state_dict().keys():
            dst_param_shape = model.state_dict()[name].shape
            if param.shape == dst_param_shape:
                model.state_dict()[name].copy_(param.view(dst_param_shape))
                net_state_keys.remove(name)
    # indicating missed keys
    if net_state_keys:
        logging.error(f">> Failed to load: {net_state_keys}")
        raise RuntimeError(f">> Failed to load: {net_state_keys}")

    model.to(device)
    model.eval()
    print("ucf101 model loaded, start to inference...")
    return model


def ppl_pre(video: Video, sampler, transforms) -> torch.Tensor:
    with tempfile.NamedTemporaryFile() as f:
        f.write(video.to_bytes())
        f.flush()
        cap = cv2.VideoCapture(f.name)
        ids = sampler.sampling(range_max=int(cap.get(cv2.CAP_PROP_FRAME_COUNT)))
        frames = []
        pre_idx = max(ids)
        for idx in ids:
            if pre_idx != (idx - 1):
                cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            res, frame = cap.read()  # in BGR/GRAY format
            pre_idx = idx
            if len(frame.shape) < 3:
                # Convert Gray to RGB
                frame = cv2.cvtColor(frame, cv2.COLOR_GRAY2RGB)
            else:
                # Convert BGR to RGB
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            frames.append(frame)
        clip_input = np.concatenate(frames, axis=2)
        trans_result = transforms(clip_input)
        # mock batch
        trans_result = trans_result[None, :]
        return (
            trans_result.float().cuda()
            if torch.cuda.is_available()
            else trans_result.float().cpu()
        )


class UCF101PipelineHandler(PipelineHandler):
    def __init__(self):
        super().__init__(ignore_error=False)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = load_model(self.device)
        self.sampler = RandomSampling()
        self.transforms = Compose(
            [
                Resize((256, 256)),
                RandomCrop((224, 224)),
                ToTensor(),
                Normalize(
                    mean=[124 / 255, 117 / 255, 104 / 255], std=[1 / (0.0167 * 255)] * 3
                ),
            ]
        )

    @torch.no_grad()
    def ppl(self, video: Video, annotations, index, **kw: t.Any) -> t.Any:
        _frames_tensor = ppl_pre(
            video=video, sampler=self.sampler, transforms=self.transforms
        )
        output = self.model(_frames_tensor)

        # recording
        probs = torch.nn.Softmax(dim=1)(output)
        label = torch.max(probs, 1)[1].detach().cpu().numpy()[0]
        print(
            f"id is:{index},real label is:{annotations},predict value is:{label}, probability is:{probs[0][label]}"
        )

        return ppl_post(output)

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
    )
    def cmp(self, ppl_result: PPLResultIterator) -> t.Any:
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["annotations"]["label"])
            pr.append(_data["result"][1])
            result.append(_data["result"][0][0])
        return label, result, pr
