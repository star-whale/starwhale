from huggingface_hub import snapshot_download

try:
    from .consts import BASE_MODEL_DIR
except ImportError:
    from consts import BASE_MODEL_DIR

if __name__ == "__main__":
    snapshot_download(repo_id="THUDM/chatglm3-6b", local_dir=BASE_MODEL_DIR)
