LLM Finetune
======

LLM finetune is a state-of-art task for large language model.

In these examples, we will use Starwhale to finetune a set of LLM base models, evaluate and release models. The demos are in the [starwhale/llm-finetuning](https://cloud.starwhale.cn/projects/401/overview) project of Starwhale Cloud.

What we learn
------

- use the `@starwhale.finetune` decorator to define a finetune handler for Starwhale Model to finish the LLM finetune.
- use the `@starwhale.evaluation.predict` to define a model evaluation for LLM.
- use the `@starwhale.handler` to define a web handler for LLM online evaluation.
- use one Starwhale Runtime to run all models.
- build Starwhale Dataset by the one-line command from the Huggingface, no code.

Models
------

- [Baichuan2](https://github.com/baichuan-inc/Baichuan2): Baichuan 2 is the new generation of open-source large language models launched by Baichuan Intelligent Technology. It was trained on a high-quality corpus with 2.6 trillion tokens.
- [ChatGLM3](https://github.com/THUDM/ChatGLM3): ChatGLM3 is a new generation of pre-trained dialogue models jointly released by Zhipu AI and Tsinghua KEG. ChatGLM3-6B is the open-source model in the ChatGLM3 series.

Datasets
------

- [Belle multiturn chat](https://huggingface.co/datasets/BelleGroup/multiturn_chat_0.8M): The dataset includes approx. 0.8M Chinese multiturn dialogs between human and assistant from BELLE Group.

    ```bash
    # build the origin dataset from huggingface
    swcli dataset build -hf BelleGroup/multiturn_chat_0.8M --name belle-multiturn-chat

    # build the random 10k items by baichuan2
    swcli dataset build --json https://raw.githubusercontent.com/baichuan-inc/Baichuan2/main/fine-tune/data/belle_chat_ramdon_10k.json --name belle_chat_random_10k
    ```

- [COIG](https://huggingface.co/datasets/BAAI/COIG): The Chinese Open Instruction Generalist (COIG) project is a harmless, helpful, and diverse set of Chinese instruction corpora.

    ```bash
    swcli dataset build -hf BAAI/COIG --name coig
    ```
