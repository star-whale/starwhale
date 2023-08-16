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

import typing as t

import gradio
import torch
from diffusers import StableDiffusionPipeline
from diffusers.loaders import LORA_WEIGHT_NAME
from starwhale import Text, PipelineHandler
from starwhale.api.service import api
try:
    from .utils import get_base_model_path, PRETRAINED_MODELS_DIR
except ImportError:
    from utils import get_base_model_path, PRETRAINED_MODELS_DIR

model_id = get_base_model_path() if get_base_model_path().exists() else "CompVis/stable-diffusion-v1-4"


class StableDiffusion(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        pipe = StableDiffusionPipeline.from_pretrained(
            model_id, torch_dtype=torch.float16
        )
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.pipe = pipe.to(device)
        # TODO judge whether exist the lora model file
        _ft_model_path = PRETRAINED_MODELS_DIR / LORA_WEIGHT_NAME
        print(f"models path:{_ft_model_path}")
        if _ft_model_path.exists():
            # load attention processors
            self.pipe.unet.load_attn_procs(_ft_model_path)

    def predict(self, data: t.Dict[str, Text]) -> t.Any:
        return self.pipe(data["text"].content).images[0]

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
