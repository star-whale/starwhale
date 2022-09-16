import os

from .invoke import invoke


class Environment:
    def __init__(self, src_dir: str, work_dir: str):
        self.src_dir = src_dir
        self.work_dir = work_dir

    def prepare(self):
        self._config_local_data_dir()
        self._install_sw()
        self._create_test_dir()
        self._download_and_extract_test_data()

    def _install_sw(self):
        invoke(["cp", "-rf", f"{self.src_dir}/client", f"{self.work_dir}/client"])

        invoke(["pip", "install", "-e", f"{self.work_dir}/client"])

    def _config_local_data_dir(self):
        os.environ["SW_CLI_CONFIG"] = f"{self.work_dir}/config.yaml"
        os.environ["SW_LOCAL_STORAGE"] = f"{self.work_dir}/data"

    def _create_test_dir(self):
        invoke(["cp", "-rf", f"{self.src_dir}/example", f"{self.work_dir}/example"])
        invoke(["rm", "-rf", f"{self.work_dir}/example/mnist/.venv"])
        invoke(["rm", "-rf", f"{self.work_dir}/example/mnist/runtime.yaml"])

    def _download_and_extract_test_data(self):
        # TODO use make
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz"])
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz"])
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz"])
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz"])
        invoke(["gzip", "-d", f"{self.work_dir}/example/mnist/data/train-images-idx3-ubyte.gz"])
        invoke(["gzip", "-d", f"{self.work_dir}/example/mnist/data/train-labels-idx1-ubyte.gz"])
        invoke(["gzip", "-d", f"{self.work_dir}/example/mnist/data/t10k-images-idx3-ubyte.gz"])
        invoke(["gzip", "-d", f"{self.work_dir}/example/mnist/data/t10k-labels-idx1-ubyte.gz"])
