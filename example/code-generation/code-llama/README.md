Code Llama
======

- 🏔️ Homepage: <https://ai.meta.com/blog/code-llama-large-language-model-coding/>
- 🌋 Github: <https://github.com/facebookresearch/codellama>
- 🤗 HuggingFace: <https://huggingface.co/codellama>
- 🏕️ Size: 7b/13b/34b
- 🎇 Introduction: Code Llama is a code-specialized version of Llama 2 that was created by further training Llama 2 on its code-specific datasets, sampling more data from that same dataset for longer. Essentially, Code Llama features enhanced coding capabilities.

In this example, we will use [CodeLlama-7b-Instruct](https://huggingface.co/codellama/CodeLlama-7b-Instruct-hf) model.

What we learn from these examples?
------

- Starwhale Web Handler.
- Integration with [transformers](https://huggingface.co/docs/transformers/index).
- Running Starwhale Model with the model builtin Starwhale Runtime.

Building Starwhale Runtime
------

```bash
swcli -vvv runtime build
swcli runtime cp code-llama https://cloud.starwhale.cn/project/starwhale:code-generation
```

Building Starwhale Model with builtin runtime
------

```bash
# download weights files from huggingface hub, need huggingface_hub lib
python3 download.py

# build Starwhale Model Package with builtin runtime
swcli model build . --runtime code-llama --name code-llama-7b-instruct -m inference

# upload Starwhale Model Package to Cloud Instance
swcli model cp code-llama-7b-instruct https://cloud.starwhale.cn/project/starwhale:code-generation
```

When using `--runtime xxx` in the `swcli model build` command, the Starwhale Model Package will include the runtime as its builtin runtime.

Running Online Evaluation in Standalone Instance
------

```bash
swcli -vvv model run --uri code-llama-7b-instruct
```

In this case, it is not necessary to configure the dataset and runtime. The web handler does not require a dataset, and the runtime parameter is optional due to the built-in runtime.

🍾Enjoy it🍾.
