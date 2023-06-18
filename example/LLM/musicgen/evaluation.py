from __future__ import annotations

import os
import typing as t
from tempfile import NamedTemporaryFile

from audiocraft.models import MusicGen
from audiocraft.data.audio import audio_write
from audiocraft.models.loaders import load_lm_model, load_compression_model

from starwhale import Audio, MIMEType, evaluation

try:
    from .utils import get_model_name, PRETRAINED_MODELS_DIR
except ImportError:
    from utils import get_model_name, PRETRAINED_MODELS_DIR

duration = int(os.environ.get("DURATION", 10))
top_k = int(os.environ.get("TOP_K", 250))
top_p = int(os.environ.get("TOP_P", 0))
temperature = float(os.environ.get("TEMPERATURE", 1.0))
cfg_coef = float(os.environ.get("CFG_COEF", 3.0))
max_input_length = int(os.environ.get("MAX_INPUT_LENGTH", 512))

_g_model = None
_g_model_name = None


def _load_model() -> t.Tuple[MusicGen, str]:
    global _g_model, _g_model_name

    if _g_model is None or _g_model_name is None:
        model_name = get_model_name()
        device = "cuda"
        c_model = load_compression_model(
            PRETRAINED_MODELS_DIR / model_name / "compression_state_dict.bin",
            device=device,
        )
        l_model = load_lm_model(
            PRETRAINED_MODELS_DIR / model_name / "state_dict.bin", device=device
        )
        if model_name == "melody":
            l_model.condition_provider.conditioners["self_wav"].match_len_on_eval = True
        _g_model = MusicGen(model_name, c_model, l_model)
        _g_model_name = model_name

    return _g_model, _g_model_name


@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
    log_dataset_features=["desc", "source_of_melody"],
)
def music_predict(data: dict) -> Audio:
    # TODO: support batch prediction
    model, _ = _load_model()
    model.set_generation_params(
        duration=duration,
        top_k=top_k,
        top_p=top_p,
        temperature=temperature,
        cfg_coef=cfg_coef,
    )
    # TODO: support melody
    outputs = model.generate([data.desc[:max_input_length]])
    output = outputs[0].detach().cpu().float()

    with NamedTemporaryFile("wb", suffix=".wav", delete=True) as file:
        fpath = audio_write(
            stem_name=file.name,
            wav=output,
            sample_rate=model.sample_rate,
            strategy="loudness",
            loudness_headroom_db=16,
            loudness_compressor=True,
            add_suffix=False,
        )
        audio = Audio(
            fp=fpath,
            mime_type=MIMEType.WAV,
        )
        return audio
