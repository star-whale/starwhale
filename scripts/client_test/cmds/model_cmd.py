from typing import Tuple

from .base.invoke import invoke
from .base.environment import CLI


class Model:
    model_cmd = "model"

    def build(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "build", "", "", "", "", "", "", ])

    def info(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "info", "", "", "", "", "", "", ])

    def list(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "list", "", "", "", "", "", "", ])

    def eval(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "eval", "", "", "", "", "", "", ])

    def copy(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "copy", "", "", "", "", "", "", ])

    def extract(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "extract", "", "", "", "", "", "", ])

    def remove(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "remove", "", "", "", "", "", "", ])

    def recover(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "recover", "", "", "", "", "", "", ])

    def history(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "history", "", "", "", "", "", "", ])

    def tag(self) -> Tuple[str, str]:
        return invoke([CLI, self.model_cmd, "tag", "", "", "", "", "", "", ])
