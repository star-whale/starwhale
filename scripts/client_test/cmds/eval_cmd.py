import json
import typing as t

from starwhale.core.eval.view import JobTermView
from starwhale.api._impl.data_store import LocalDataStore

from . import CLI
from .base.invoke import invoke


class Evaluation:
    _cmd = "eval"

    def run(
        self,
        model: str,
        datasets: t.List[str],
        project: str = "self",
        version: str = "",
        runtime: str = "",
        name: str = "",
        desc: str = "",
        step_spec: str = "",
        use_docker: bool = False,
        gencmd: bool = False,
        step: str = "",
        task_index: int = 0,
        resource_pool: str = "",
    ) -> str:
        """
        :param project:
        :param version: Evaluation job version
        :param model: model uri or model.yaml dir path  [required]
        :param datasets: dataset uri, one or more  [required]
        :param runtime: runtime uri
        :param name: job name
        :param desc: job description
        :param step_spec: [ONLY Cloud]yaml format
        :param use_docker: [ONLY Standalone]use docker to run evaluation job
        :param gencmd: [ONLY Standalone]gen docker run command
        :param step: Evaluation run step
        :param task_index:
        :param resource_pool: [ONLY Cloud] which nodes should job run on
        :return:
        """

        jid = JobTermView.run(
            project,
            model,
            datasets,
            runtime,
            version=version,
            name=name,
            desc=desc,
            step_spec=step_spec,
            resource_pool=resource_pool,
            gencmd=gencmd,
            use_docker=use_docker,
            step=step,
            task_index=task_index,
        )
        LocalDataStore.get_instance().dump()
        return jid

    def info(self, version: str) -> t.Any:
        """
        :param version:
        :return:
            local:{
                    "manifest": {
                        "created_at": "2022-09-15 15:10:08 CST",
                        "datasets": [
                            "local/project/self/dataset/mnist/version/latest"
                        ],
                        "desc": null,
                        "finished_at": "2022-09-15 15:10:29 CST",
                        "model": "mnist/version/latest",
                        "model_dir": "/home/star_g/.starwhale/self/workdir/model/mnist/g4/g4ytkzbwge2dinrtmftdgyjznazhczy/src",
                        "name": "default",
                        "project": "self",
                        "runtime": "",
                        "status": "success",
                        "step": "",
                        "task_index": 0,
                        "version": "gzswgmlgmeztemzxme4gkzldgm2xqny"
                    },
                    "report": {
                        "confusion_matrix": {
                            "binarylabel": [
                                {
                                    "0": 0.0977,
                                    "1": 0.0,
                                    "2": 0.0,
                                    "3": 0.0,
                                    "4": 0.0,
                                    "5": 0.0,
                                    "6": 0.0001,
                                    "7": 0.0001,
                                    "8": 0.0001,
                                    "9": 0.0,
                                    "id": 0
                                },
                                ...
                            ]
                        },
                        "kind": "multi_classification",
                        "labels": {
                            "0": {
                                "f1-score": 0.9903699949315762,
                                "id": "0",
                                "precision": 0.9838872104733132,
                                "recall": 0.996938775510204,
                                "support": 980
                            },
                            ...
                        },
                        "summary": {
                            "accuracy": 0.9894,
                            "id": "gzswgmlgmeztemzxme4gkzldgm2xqny",
                            "kind": "multi_classification",
                            "macro avg/f1-score": 0.9893068278560861,
                            "macro avg/precision": 0.9893024234074879,
                            "macro avg/recall": 0.9893615007448199,
                            "macro avg/support": 10000,
                            "weighted avg/f1-score": 0.9893988328111353,
                            "weighted avg/precision": 0.9894464486215673,
                            "weighted avg/recall": 0.9894,
                            "weighted avg/support": 10000
                        }
                    }
                }
            cloud:{
                    "manifest": {
                        "comment": null,
                        "createdTime": 1663297648000,
                        "datasets": [
                            "gi4toodchfsggnrtmftdgyjzpjtho2y"
                        ],
                        "device": "CPU",
                        "deviceAmount": 1000,
                        "duration": 57000,
                        "id": "72",
                        "jobStatus": "FAIL",
                        "modelName": "mnist",
                        "modelVersion": "g4ytkzbwge2dinrtmftdgyjznazhczy",
                        "owner": {
                            "createdTime": 1650970583000,
                            "id": "1",
                            "isEnabled": true,
                            "name": "starwhale",
                            "projectRoles": null,
                            "systemRole": null
                        },
                        "resourcePool": "default",
                        "runtime": {
                            "createdTime": 1661483449000,
                            "id": "9",
                            "name": "pytorch",
                            "owner": {
                                "createdTime": 1650970583000,
                                "id": "1",
                                "isEnabled": true,
                                "name": "starwhale",
                                "projectRoles": null,
                                "systemRole": null
                            },
                            "version": {
                                "alias": "v37",
                                "createdTime": 1663296946000,
                                "id": "39",
                                "image": "ghcr.io/star-whale/starwhale:latest-cuda11.4",
                                "meta": "",
                                "name": "mjqtinbtgjqtezjyg44tkzjwnm2gmny",
                                "owner": {
                                    "createdTime": 1650970583000,
                                    "id": "1",
                                    "isEnabled": true,
                                    "name": "starwhale",
                                    "projectRoles": null,
                                    "systemRole": null
                                },
                                "tag": null
                            }
                        },
                        "stopTime": 1663297705000,
                        "uuid": "67c88413bf654b49858d001280c58cc7"
                    },
                    "report": {
                        "kind": "",
                        "summary": {}
                    },
                    "tasks": [
                        [
                            {
                                "createdTime": -1,
                                "created_at": "1970-01-01 07:59:59",
                                "id": "168",
                                "resourcePool": "default",
                                "taskStatus": "CREATED",
                                "uuid": "9ae3e99d-2761-4d84-bc35-87a17f71ae81"
                            },
                            {
                                "createdTime": 1663297650000,
                                "created_at": "2022-09-16 11:07:30",
                                "id": "167",
                                "resourcePool": "default",
                                "taskStatus": "FAIL",
                                "uuid": "62c52e86-2b74-42bc-b2c3-50c44a41abc7"
                            }
                        ],
                        {
                            "current": 2,
                            "remain": 0,
                            "total": 2
                        }
                    ]
                }
        """
        _ret_code, _res = invoke([CLI, "-o", "json", self._cmd, "info", version])
        return json.loads(_res) if _ret_code == 0 else {}

    def list(
        self,
        project: str = "self",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = 1,
        size: int = 20,
    ) -> t.Any:
        """

        :param project:
        :param fullname:
        :param show_removed:
        :param page:
        :param size:
        :return:
            [
                {
                    "is_removed": false,
                    "location": "/home/star_g/.starwhale/self/evaluation/gr/grrggnrzmiztmnzwgjrwemdfgu4hc5y/_manifest.yaml",
                    "manifest": {
                        "created_at": "2022-08-23 17:16:39 CST",
                        "datasets": [
                            "mnist/version/latest"
                        ],
                        "finished_at": "2022-08-23 17:16:45 CST",
                        "model_dir": ".",
                        "project": "self",
                        "status": "failed",
                        "step": "",
                        "task_index": 0,
                        "version": "grrggnrzmiztmnzwgjrwemdfgu4hc5y"
                    }
                },
                ...
            ]
        """
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

        _ret_code, _res = invoke(_args)
        return json.loads(_res) if _ret_code == 0 else []

    def cancel(self, uri: str, force: bool = False) -> bool:
        return self._operate("cancel", uri=uri, force=force)

    def compare(self, base_uri: str, compare_uri: str) -> t.Any:
        _ret_code, _res = invoke([CLI, self._cmd, "compare", base_uri, compare_uri])
        return _res

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
        _ret_code, _res = invoke(_args)
        return bool(_ret_code == 0)
