import os
import tempfile

from .invoke import invoke

CLI = "swcli"


class Environment:
    def __init__(self, src_dir: str):
        self.src_dir = src_dir
        self.tmp_dir = tempfile.TemporaryDirectory()
        self.work_dir: str = self.tmp_dir.name
        self._init_local_data_dir()
        self._install_sw()
        self._create_test_dir()
        self._download_test_data()

    def _install_sw(self):
        invoke(["cp", "-rf", f"{self.src_dir}/client", f"{self.work_dir}/client"])

        invoke(["pip", "install", "-e", f"{self.work_dir}/client"])

    def _init_local_data_dir(self):
        os.environ["SW_CLI_CONFIG"] = f"{self.work_dir}/config.yaml"
        os.environ["SW_LOCAL_STORAGE"] = f"{self.work_dir}/data"

    def _create_test_dir(self):
        invoke(["cp", "-rf", f"{self.src_dir}/example", f"{self.work_dir}/example"])

    def _download_test_data(self):
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz"])
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz"])
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz"])
        invoke(["wget", "-P", f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz"])
        invoke(["gzip", "-d", "--", f"{self.work_dir}/example/mnist/data/*.gz"])

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.tmp_dir.cleanup()
