from __future__ import annotations

import typing as t
from pathlib import Path

import torch
import gradio
from transformers import AutoTokenizer, AutoModelForCausalLM

from starwhale import handler

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"

_g_model = None
_g_tokenizer = None

PROMPT_TEMPLATE = "<s>[INST] {input} [/INST]"

# TODO: support vllm


def _load_model_and_tokenizer() -> t.Tuple:
    global _g_model, _g_tokenizer

    if _g_model is None:
        print(f"load model {PRETRAINED_MODELS_DIR} into memory...")
        _g_model = AutoModelForCausalLM.from_pretrained(
            PRETRAINED_MODELS_DIR, torch_dtype=torch.float16, device_map="auto"
        )

    if _g_tokenizer is None:
        print(f"load tokenizer {PRETRAINED_MODELS_DIR} into memory...")
        _g_tokenizer = AutoTokenizer.from_pretrained(PRETRAINED_MODELS_DIR)

    return _g_model, _g_tokenizer


def code_copilot(
    prompt: str,
    temperature: float = 0.7,
    top_p: float = 0.9,
    repetition_penalty: float = 1.05,
    max_new_tokens: int = 1024,
) -> str:
    model, tokenizer = _load_model_and_tokenizer()

    prompt = PROMPT_TEMPLATE.format(input=prompt)
    input_ids = tokenizer(
        prompt, return_tensors="pt", add_special_tokens=False
    ).input_ids.to(model.device)
    output = model.generate(
        input_ids,
        max_new_tokens=max_new_tokens,
        temperature=temperature,
        top_p=top_p,
        repetition_penalty=repetition_penalty,
        do_sample=temperature > 0,
    )
    output = output[0].to("cpu")
    return tokenizer.decode(output[input_ids.shape[1] :], skip_special_tokens=True)


gradio_theme = gradio.themes.Monochrome(
    primary_hue="indigo",
    secondary_hue="blue",
    neutral_hue="slate",
    radius_size=gradio.themes.sizes.radius_sm,
    font=[
        gradio.themes.GoogleFont("Open Sans"),
        "ui-sans-serif",
        "system-ui",
        "sans-serif",
    ],
)

examples = [
    "Write a quicksort in Python",
    "In Bash, how do I find the 10 largest files in a directory?",
    "I have a list of numbers, how do I sort them in Python?",
    "def merge_sort(list): # Merge sort in python",
    "Can you explain how to use the map function in Python?",
    "Write a unit test for this function: $(cat fib.py)",
    "X_train, y_train, X_test, y_test = train_test_split(X, y, test_size=0.1)\n\n# Train a logistic regression model, predict the labels on the test set and compute the accuracy score",
    "// Returns every other value in the array as a new array.\nfunction everyOther(arr) {",
    "def alternating(list1, list2):\n   results = []\n   for i in range(min(len(list1), len(list2))):\n       results.append(list1[i])\n       results.append(list2[i])\n   if len(list1) > len(list2):\n       <FILL_ME>\n   else:\n       results.extend(list2[i+1:])\n   return results",
    'def remove_non_ascii(s: str) -> str:\n    """ <FILL_ME>\nprint(remove_non_ascii(\'afkdj$$(\'))',
    "Given an integer array nums, return all the triplets [nums[i], nums[j], nums[k]] such that i != j, i != k, and j != k, and nums[i] + nums[j] + nums[k] == 0. Notice that the solution set must not contain duplicate triplets.",
    "What is the difference between inorder and preorder traversal? Give an example in Python.",
    "In Bash, how do I list all text files in the current directory (excluding subdirectories) that have been modified in the last month?",
    "Provide answers in JavaScript. Write a function that computes the set of sums of all contiguous sublists of a given list.",
]

css = """
.generating {visibility: hidden}
#q-input textarea {
    font-family: monospace, 'Consolas', Courier, monospace;
}
.gradio-container {color: black}
"""


# handler is a decorator that registers the function as a handler for the specified resources.
#   - expose: the port to expose the service on, we use expose arguments to specify web handler.
#   - resources: cpu, memory, gpu, etc.
#   - handler does not need datasets.
@handler(expose=17860, resources={"nvidia.com/gpu": 1})
def playground() -> None:
    description = """
Code Llama Playground
======

This is a Code Llama playground powered by [Starwhale](https://github.com/star-whale/starwhale), a code assistant powered by Code-Llama-7b-Instruct-hf model.
"""
    with gradio.Blocks(theme=gradio_theme, analytics_enabled=False, css=css) as server:
        with gradio.Column():
            gradio.Markdown(description)
            with gradio.Row():
                with gradio.Column():
                    prompt = gradio.Textbox(
                        label="Prompt",
                        lines=5,
                        placeholder="Type your prompt here",
                        elem_id="q-prompt",
                    )
                    gradio.Examples(
                        examples=examples,
                        inputs=[prompt],
                        cache_examples=False,
                    )
                    submit = gradio.Button("Generate", variant="primary")
                    output = gradio.Code(
                        label="Code Output", lines=20, readonly=True, elem_id="q-output"
                    )

                    with gradio.Row():
                        with gradio.Column():
                            with gradio.Accordion("Advanced settings", open=False):
                                with gradio.Row():
                                    c1, c2 = gradio.Column(), gradio.Column()
                                    with c1:
                                        temperature = gradio.Slider(
                                            label="Temperature",
                                            value=0.1,
                                            minimum=0.0,
                                            maximum=1.0,
                                            step=0.05,
                                            interactive=True,
                                            info="Higher values produce more diverse outputs",
                                        )
                                        max_new_tokens = gradio.Slider(
                                            label="Max new tokens",
                                            value=1024,
                                            minimum=0,
                                            maximum=8192,
                                            step=64,
                                            interactive=True,
                                            info="The maximum numbers of new tokens",
                                        )
                                    with c2:
                                        top_p = gradio.Slider(
                                            label="Top-p (nucleus sampling)",
                                            value=0.90,
                                            minimum=0.0,
                                            maximum=1,
                                            step=0.05,
                                            interactive=True,
                                            info="Higher values sample more low-probability tokens",
                                        )
                                        repetition_penalty = gradio.Slider(
                                            label="Repetition penalty",
                                            value=1.05,
                                            minimum=1.0,
                                            maximum=2.0,
                                            step=0.05,
                                            interactive=True,
                                            info="Penalize repeated tokens",
                                        )

        submit.click(
            code_copilot,
            inputs=[prompt, temperature, top_p, repetition_penalty, max_new_tokens],
            outputs=[output],
        )

    server.launch(server_name="0.0.0.0", server_port=17860, share=True, debug=True)
