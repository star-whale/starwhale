from huggingface_hub import snapshot_download

import starwhale

try:
    from .utils import BASE_MODEL_DIR
    from .finetune import lora_finetune
    from .evaluation import chatbot, copilot_predict
except ImportError:
    from utils import BASE_MODEL_DIR
    from finetune import lora_finetune
    from evaluation import chatbot, copilot_predict

starwhale.init_logger(3)


def build_starwhale_model() -> None:
    BASE_MODEL_DIR.mkdir(parents=True, exist_ok=True)

    snapshot_download(
        repo_id="baichuan-inc/Baichuan2-7B-Chat",
        local_dir=BASE_MODEL_DIR,
    )

    starwhale.model.build(
        name="baichuan2-7b-chat",
        modules=[copilot_predict, chatbot, lora_finetune],
    )


if __name__ == "__main__":
    build_starwhale_model()
