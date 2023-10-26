from __future__ import annotations

import os
import typing as t
from tempfile import NamedTemporaryFile

from transformers import AutoProcessor, MusicgenForConditionalGeneration
from audiocraft.data.audio import audio_write

from starwhale import Audio, MIMEType, evaluation

try:
    from .utils import get_model_name, PRETRAINED_MODELS_DIR
except ImportError:
    from utils import get_model_name, PRETRAINED_MODELS_DIR

top_k = int(os.environ.get("TOP_K", 250))
top_p = int(os.environ.get("TOP_P", 0))
temperature = float(os.environ.get("TEMPERATURE", 1.0))
max_input_length = int(os.environ.get("MAX_INPUT_LENGTH", 512))
max_new_tokens = int(os.environ.get("MAX_NEW_TOKENS", 500))  # 500 tokens = 10 seconds
guidance_scale = float(os.environ.get("GUIDANCE_SCALE", 3.0))

_g_model = None
_g_processor = None
_g_model_name = None


def _load_model() -> t.Tuple[t.Any, t.Any, str]:
    global _g_model, _g_model_name, _g_processor

    if _g_model is None or _g_model_name is None or _g_processor is None:
        _g_model_name = get_model_name()
        _g_model = MusicgenForConditionalGeneration.from_pretrained(
            PRETRAINED_MODELS_DIR / _g_model_name
        )
        _g_model.to("cuda")
        _g_processor = AutoProcessor.from_pretrained(
            PRETRAINED_MODELS_DIR / _g_model_name
        )
    return _g_model, _g_processor, _g_model_name


@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
    log_dataset_features=["desc", "source_of_melody"],
)
def music_predict(data: dict) -> Audio:
    # TODO: support batch prediction
    model, processor, _ = _load_model()
    inputs = processor(
        text=[data["desc"][:max_input_length]],
        padding=True,
        return_tensors="pt",
    )
    # TODO: support melody
    outputs = model.generate(
        **inputs.to("cuda"),
        do_sample=True,
        guidance_scale=guidance_scale,
        max_new_tokens=max_new_tokens,
        top_k=top_k,
        top_p=top_p,
        temperature=temperature,
    )
    output = outputs[0].detach().cpu().float()

    with NamedTemporaryFile("wb", suffix=".wav", delete=True) as file:
        fpath = audio_write(
            stem_name=file.name,
            wav=output,
            sample_rate=model.config.audio_encoder.sampling_rate,
            strategy="loudness",
            loudness_headroom_db=16,
            loudness_compressor=True,
            add_suffix=False,
        )
        audio = Audio(
            fp=fpath.read_bytes(),
            mime_type=MIMEType.WAV,
        )
        return audio
