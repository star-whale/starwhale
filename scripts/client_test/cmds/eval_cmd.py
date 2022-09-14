from typing import Tuple

from base.invoke import invoke


class Evaluation:
    def run(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def info(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def list(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def cancel(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def compare(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def pause(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def remove(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def recover(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])

    def resume(self) -> Tuple[str, str]:
        return invoke(["", "", "", "", "", "", "", ])
