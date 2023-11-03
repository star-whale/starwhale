#  Copyright 2022 Starwhale, Inc. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import io
import typing as t

import gradio
import torch
from diffusers import StableDiffusionPipeline, DPMSolverMultistepScheduler
from diffusers.loaders import LORA_WEIGHT_NAME
from starwhale import Text, PipelineHandler, Image, MIMEType
from starwhale.api.service import api
from starwhale.base.type import PredictLogMode

try:
    from .utils import get_base_model_path, PRETRAINED_MODELS_DIR
except ImportError:
    from utils import get_base_model_path, PRETRAINED_MODELS_DIR

model_id = get_base_model_path() if get_base_model_path().exists() else "CompVis/stable-diffusion-v1-4"


class StableDiffusion(PipelineHandler):
    def __init__(self) -> None:
        super().__init__(predict_log_mode=PredictLogMode.PLAIN)
        pipe = StableDiffusionPipeline.from_pretrained(
            model_id, torch_dtype=torch.float16
        )
        pipe.scheduler = DPMSolverMultistepScheduler.from_config(pipe.scheduler.config)
        self.cross_attention_kwargs = {}
        # whether exist the lora model file
        _lora_model_path = PRETRAINED_MODELS_DIR / LORA_WEIGHT_NAME
        if _lora_model_path.exists():
            # load attention processors
            pipe.unet.load_attn_procs(_lora_model_path, use_safetensors=False)
            self.cross_attention_kwargs = {"scale": 0.5}
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.pipe = pipe.to(device)

    def predict(self, data: t.Dict[str, str]) -> t.Any:
        img = self.pipe(
                data["text"], guidance_scale=7.5, cross_attention_kwargs=self.cross_attention_kwargs
            ).images[0]
        bytes = io.BytesIO()
        img.save(bytes, format='PNG')
        return Image(
            fp=bytes.getvalue(),
            mime_type=MIMEType.PNG,
        )

    def evaluate(self, ppl_result: t.Iterator) -> t.Any:
        return ppl_result

    @api(
        [
            gradio.Text(label="prompt"),
            gradio.Text(label="negative prompt"),
            gradio.Slider(minimum=0, maximum=50, label="Guidance Scale", value=9),
        ],
        output=gradio.Image(),
    )
    def txt2img(self, prompt: str, negative_prompt: str, guidance_scale: float):
        return self.pipe(
            prompt, negative_prompt=negative_prompt, guidance_scale=guidance_scale
        ).images[0]
