from .invoke import invoke


class EnvironmentPrepare:
    def __init__(self, work_dir: str) -> None:
        self.work_dir = work_dir

    def prepare_mnist_requirements(self) -> None:
        _res, _err = invoke(
            [
                "python3",
                "-m",
                "pip",
                "install",
                "-r",
                f"{self.work_dir}/example/mnist/requirements-sw-lock.txt",
            ]
        )
        print(f"install package info:{_res}, err is:{_err}")

    def prepare_mnist_data(self) -> None:
        # TODO use make
        invoke(
            [
                "wget",
                "-P",
                f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz",
            ]
        )
        invoke(
            [
                "wget",
                "-P",
                f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz",
            ]
        )
        invoke(
            [
                "wget",
                "-P",
                f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz",
            ]
        )
        invoke(
            [
                "wget",
                "-P",
                f"{self.work_dir}/example/mnist/data",
                "http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz",
            ]
        )
        invoke(
            [
                "gzip",
                "-d",
                f"{self.work_dir}/example/mnist/data/train-images-idx3-ubyte.gz",
            ]
        )
        invoke(
            [
                "gzip",
                "-d",
                f"{self.work_dir}/example/mnist/data/train-labels-idx1-ubyte.gz",
            ]
        )
        invoke(
            [
                "gzip",
                "-d",
                f"{self.work_dir}/example/mnist/data/t10k-images-idx3-ubyte.gz",
            ]
        )
        invoke(
            [
                "gzip",
                "-d",
                f"{self.work_dir}/example/mnist/data/t10k-labels-idx1-ubyte.gz",
            ]
        )
