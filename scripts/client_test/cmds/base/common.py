from os.path import exists

from .invoke import check_invoke, invoke_with_react


class EnvironmentPrepare:
    def __init__(self, work_dir: str) -> None:
        self.work_dir = work_dir

    def prepare_mnist_requirements(self) -> None:
        check_invoke(
            [
                "python3",
                "-m",
                "pip",
                "install",
                "-r",
                f"{self.work_dir}/example/mnist/requirements-sw-lock.txt",
            ],
            log=True,
        )

    def download(self, package: str) -> None:
        if not exists(f"{self.work_dir}/example/mnist/data/{package}"):
            check_invoke(
                [
                    "wget",
                    "-P",
                    f"{self.work_dir}/example/mnist/data",
                    f"http://yann.lecun.com/exdb/mnist/{package}",
                ]
            )
        invoke_with_react(
            [
                "gzip",
                "-d",
                f"{self.work_dir}/example/mnist/data/{package}",
            ]
        )

    def prepare_mnist_data(self) -> None:
        # TODO use make
        packages = [
            "t10k-images-idx3-ubyte.gz",
            "t10k-labels-idx1-ubyte.gz",
        ]
        for pkg in packages:
            self.download(pkg)
