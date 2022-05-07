from pathlib import Path

from starwhale.base.store import LocalStorage


class EvalLocalStorage(LocalStorage):

    def list(self, filter: str = "", title: str = "", caption: str = "") -> None:
        pass

    def iter_local_swobj(self) -> "LocalStorage.SWobjMeta":
        pass

    def push(self, sw_name: str) -> None:
        pass

    def pull(self, sw_name: str) -> None:
        pass

    def info(self, sw_name: str) -> None:
        pass

    def delete(self, sw_name: str) -> None:
        pass

    def gc(self, dry_run: bool = False) -> None:
        pass