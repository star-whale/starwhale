import json
import typing as t

from . import CLI
from .base.invoke import invoke_output, invoke_ret_code


class Job:
    _cmd = "job"

    def info(self, version: str) -> t.Any:
        _ret_code, _res = invoke_output(
            [CLI, "-o", "json", self._cmd, "info", version], log=True
        )
        try:
            return json.loads(_res) if _ret_code == 0 else {}
        except Exception as e:
            print(
                f"failed to get job info[{version}]: {e}, ret-code:{_ret_code}, res:{_res}"
            )
            raise

    def list(
        self,
        project: str = "self",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = 1,
        size: int = 20,
    ) -> t.Any:
        _args = [
            CLI,
            "-o",
            "json",
            self._cmd,
            "list",
            "--page",
            str(page),
            "--size",
            str(size),
        ]
        if project:
            _args.extend(["--project", project])
        if fullname:
            _args.append("--fullname")
        if show_removed:
            _args.append("--show-removed")

        _ret_code, _res = invoke_output(_args)
        return json.loads(_res.strip()) if _ret_code == 0 else []

    def cancel(self, uri: str, force: bool = False) -> bool:
        return self._operate("cancel", uri=uri, force=force)

    def pause(self, uri: str, force: bool = False) -> bool:
        return self._operate("pause", uri=uri, force=force)

    def remove(self, uri: str, force: bool = False) -> bool:
        return self._operate("remove", uri=uri, force=force)

    def recover(self, uri: str, force: bool = False) -> bool:
        return self._operate("recover", uri=uri, force=force)

    def resume(self, uri: str, force: bool = False) -> bool:
        return self._operate("resume", uri=uri, force=force)

    def _operate(self, name: str, uri: str, force: bool = False) -> bool:
        """
        :param uri: version or uri(evaluation/{version})
        :param force: bool
        :return:
        """
        _args = [CLI, self._cmd, name, uri]
        if force:
            _args.append("--force")
        _code = invoke_ret_code(_args)
        return _code == 0
