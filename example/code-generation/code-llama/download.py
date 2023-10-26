from __future__ import annotations

from pathlib import Path

from huggingface_hub import snapshot_download

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"


def download(repo_id: str) -> None:
    print(f"try to download model from {repo_id}")
    snapshot_download(
        repo_id=repo_id,
        local_dir=PRETRAINED_MODELS_DIR,
    )


if __name__ == "__main__":
    download("codellama/CodeLlama-7b-Instruct-hf")
