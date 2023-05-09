import typing as t

import torch
import gradio
from diffusers import StableDiffusionPipeline, DPMSolverMultistepScheduler

from starwhale import Text, PipelineHandler
from starwhale.api.service import api

model_id = "stabilityai/stable-diffusion-2"


class StableDiffusion(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        pipe = StableDiffusionPipeline.from_pretrained(
            model_id, torch_dtype=torch.float16
        )
        pipe.scheduler = DPMSolverMultistepScheduler.from_config(pipe.scheduler.config)
        self.pipe = pipe.to("cuda")

    def ppl(self, content: Text) -> t.Any:
        return self.pipe(content).images[0]

    def cmp(self, ppl_result: t.Iterator) -> t.Any:
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
