import json
from typing import Tuple, Dict, Any, List

from . import CLI
from .base.invoke import invoke


class Evaluation:
    _cmd = "eval"

    def run(self,
            model: str,
            dataset: str,
            project: str = "self",
            version: str = "",
            runtime: str = "",
            name: str = "",
            desc: str = "",
            resource: str = "",
            use_docker: bool = False,
            gencmd: bool = False,
            step: str = "",
            task_index: int = 0, ) -> bool:
        """
        :param project:
        :param version: Evaluation job version
        :param model: model uri or model.yaml dir path  [required]
        :param dataset: dataset uri, one or more  [required]
        :param runtime: runtime uri
        :param name: job name
        :param desc: job description
        :param resource: [ONLY Cloud]resource, fmt is resource [name]:[cnt],
                         such as cpu:1, gpu:2
        :param use_docker: [ONLY Standalone]use docker to run evaluation job
        :param gencmd: [ONLY Standalone]gen docker run command
        :param step: Evaluation run step
        :param task_index:
        :return:
        """
        _valid_str = "success to create job"
        _args = [CLI, self._cmd, "run", "--model", model, "--dataset", dataset]
        if version:
            _args.extend(["--version", version])
        if runtime:
            _args.extend(["--runtime", runtime])
        if name:
            _args.extend(["--name", name])
        if desc:
            _args.extend(["--desc", desc])
        if resource:
            _args.extend(["--resource", resource])
        if use_docker:
            _args.append("--use-docker")
        # TODO: return value is str
        if gencmd:
            _args.append("--gencmd")
        if step:
            _args.extend(["--step", step, "--task-index", task_index])
        _args.append(project)
        _res, _err = invoke(_args)
        return not _err and _valid_str in _res

    def info(self, version: str) -> Dict[str, Any]:
        """
        :param version:
        :return:
            {
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
        """
        _res, _err = invoke([CLI, "-o", "json", self._cmd, "info", version])
        return json.loads(_res) if not _err else {}

    def list(self,
             project: str = "self",
             fullname: bool = False,
             show_removed: bool = False,
             page: int = 1,
             size: int = 20, ) -> List[Dict[str, Any]]:
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
                        "model": "models/mnist_cnn.pt",
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
        _args = [CLI, "-o", "json", self._cmd, "list", "--page", str(page), "--size", str(size)]
        if project:
            _args.extend(["--project", project])
        if fullname:
            _args.append("--fullname")
        if show_removed:
            _args.append("--show-removed")

        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else []

    def cancel(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "cancel", "", "", "", "", "", "", ])

    def compare(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "compare", "", "", "", "", "", "", ])

    def pause(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "pause", "", "", "", "", "", "", ])

    def remove(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "remove", "", "", "", "", "", "", ])

    def recover(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "recover", "", "", "", "", "", "", ])

    def resume(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "resume", "", "", "", "", "", "", ])
