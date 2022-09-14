from typing import Tuple

from base.invoke import invoke


class Model:
    def build(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def info(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def list(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def eval(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def copy(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def extract(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def remove(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def recover(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def history(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def tag(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])
