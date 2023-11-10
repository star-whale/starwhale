from pathlib import Path

from huggingface_hub import snapshot_download

from starwhale import model as starwhale_model
from starwhale import init_logger

try:
    from . import inference  # noqa: F401
except ImportError:
    import inference  # noqa: F401

init_logger(3)

ROOT_DIR = Path(__file__).parent
MODEL_DIR = ROOT_DIR / "models"


def build_starwhale_model():
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    snapshot_download(
        repo_id="aaraki/vit-base-patch16-224-in21k-finetuned-cifar10",
        local_dir=MODEL_DIR,
    )

    # if modules is not specified, the search modules are the imported modules.
    starwhale_model.build(name="vit-finetuned-cifar10")


if __name__ == "__main__":
    build_starwhale_model()
