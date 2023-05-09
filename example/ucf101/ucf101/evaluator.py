import os
import typing as t
import logging
import tempfile
from pathlib import Path

import cv2
import numpy as np
import torch
import gradio

from starwhale import Video, PipelineHandler, multi_classification
from starwhale.api.service import api

from .model import MFNET_3D
from .sampler import RandomSampling
from .transform import Resize, Compose, ToTensor, Normalize, RandomCrop

root_dir = Path(__file__).parent.parent
_LABELS = (
    "ApplyEyeMakeup",
    "ApplyLipstick",
    "Archery",
    "BabyCrawling",
    "BalanceBeam",
    "BandMarching",
    "BaseballPitch",
    "Basketball",
    "BasketballDunk",
    "BenchPress",
    "Biking",
    "Billiards",
    "BlowDryHair",
    "BlowingCandles",
    "BodyWeightSquats",
    "Bowling",
    "BoxingPunchingBag",
    "BoxingSpeedBag",
    "BreastStroke",
    "BrushingTeeth",
    "CleanAndJerk",
    "CliffDiving",
    "CricketBowling",
    "CricketShot",
    "CuttingInKitchen",
    "Diving",
    "Drumming",
    "Fencing",
    "FieldHockeyPenalty",
    "FloorGymnastics",
    "FrisbeeCatch",
    "FrontCrawl",
    "GolfSwing",
    "Haircut",
    "Hammering",
    "HammerThrow",
    "HandstandPushups",
    "HandstandWalking",
    "HeadMassage",
    "HighJump",
    "HorseRace",
    "HorseRiding",
    "HulaHoop",
    "IceDancing",
    "JavelinThrow",
    "JugglingBalls",
    "JumpingJack",
    "JumpRope",
    "Kayaking",
    "Knitting",
    "LongJump",
    "Lunges",
    "MilitaryParade",
    "Mixing",
    "MoppingFloor",
    "Nunchucks",
    "ParallelBars",
    "PizzaTossing",
    "PlayingCello",
    "PlayingDaf",
    "PlayingDhol",
    "PlayingFlute",
    "PlayingGuitar",
    "PlayingPiano",
    "PlayingSitar",
    "PlayingTabla",
    "PlayingViolin",
    "PoleVault",
    "PommelHorse",
    "PullUps",
    "Punch",
    "PushUps",
    "Rafting",
    "RockClimbingIndoor",
    "RopeClimbing",
    "Rowing",
    "SalsaSpin",
    "ShavingBeard",
    "Shotput",
    "SkateBoarding",
    "Skiing",
    "Skijet",
    "SkyDiving",
    "SoccerJuggling",
    "SoccerPenalty",
    "StillRings",
    "SumoWrestling",
    "Surfing",
    "Swing",
    "TableTennisShot",
    "TaiChi",
    "TennisSwing",
    "ThrowDiscus",
    "TrampolineJumping",
    "Typing",
    "UnevenBars",
    "VolleyballSpiking",
    "WalkingWithDog",
    "WallPushups",
    "WritingOnBoard",
    "YoYo",
)


def ppl_post(output: torch.Tensor) -> t.List[t.Tuple[str, t.List[float]]]:
    pred_value = output.argmax(-1).flatten().tolist()
    probability_matrix = torch.nn.Softmax(dim=1)(output).tolist()
    return list(zip([str(p) for p in pred_value], probability_matrix))


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


def ppl_pre(videos: t.List[Video], sampler, transforms) -> torch.Tensor:
    trans_results = []
    for video in videos:
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
            trans_results.append(transforms(clip_input))

    return (
        torch.stack(trans_results).float().cuda()
        if torch.cuda.is_available()
        else torch.stack(trans_results).float().cpu()
    )


class UCF101PipelineHandler(PipelineHandler):
    def __init__(self):
        super().__init__(ignore_error=False, predict_batch_size=5)
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
    def ppl(self, data_batch: t.List[dict]) -> t.Any:
        _frames_tensor = ppl_pre(
            videos=[data["video"] for data in data_batch],
            sampler=self.sampler,
            transforms=self.transforms,
        )
        output = self.model(_frames_tensor)
        return ppl_post(output)

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
    )
    def cmp(self, ppl_result: t.Iterator) -> t.Any:
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["ds_data"]["label"])
            result.append(_data["result"][0])
            pr.append(_data["result"][1])
        return label, result, pr

    @api(
        gradio.Video(type="filepath"),
        gradio.Label(num_top_classes=5),
        examples=[os.path.join(os.path.dirname(__file__), "../taichi.webm")],
    )
    def online_eval(self, file: str):
        with open(file, "rb") as f:
            data = f.read()
        prob = self.ppl([Video(fp=data)])[0]
        return {_LABELS[i]: p for i, p in enumerate(prob[1])}
