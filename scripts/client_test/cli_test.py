import os
import sys
import logging
import tempfile
from time import sleep
from typing import Any, Optional

from cmds.eval_cmd import Evaluation
from cmds.base.common import EnvironmentPrepare
from cmds.base.invoke import invoke
from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.artifacts_cmd import Model, Dataset, Runtime

CURRENT_DIR = os.path.dirname(__file__)
SCRIPT_DIR = os.path.abspath(os.path.join(CURRENT_DIR, os.pardir))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, os.pardir))

logging.basicConfig(level=logging.DEBUG)


class TestCli:
    instance = Instance()
    project = Project()
    model = Model()
    dataset = Dataset()
    runtime = Runtime()
    evaluation = Evaluation()

    def __init__(self, work_dir: str) -> None:
        self._work_dir = work_dir

    def standard_workflow(
        self,
        mode: str,
        model_name: str,
        model_workdir: str,
        ds_name: str,
        ds_workdir: str,
        rt_name: str,
        rt_workdir: str,
        server_uri: str,
        server_project: str,
        step_spec_file: str,
    ) -> None:
        # use local instance
        logging.info("select local")
        self.instance.select("local")
        assert self.project.select("self")

        # 1.model build
        logging.info("building model...")
        _model_uri = f"{model_name}/version/latest"
        assert len(self.model.list()) == 0
        self.model.build(workdir=model_workdir)
        assert len(self.model.list()) == 1
        swmp = self.model.info(_model_uri)
        assert swmp

        # 2.dataset build
        logging.info("building dataset...")
        _ds_uri = f"{ds_name}/version/latest"
        assert len(self.dataset.list()) == 0
        self.dataset.build(workdir=ds_workdir)
        assert len(self.dataset.list()) == 1
        swds = self.dataset.info(_ds_uri)
        assert swds
        self.dataset.build(workdir=ds_workdir)
        assert len(self.dataset.list()) == 2
        swds2 = self.dataset.info(_ds_uri)
        assert swds2

        # 3.runtime build
        logging.info("building runtime...")
        _rt_uri = f"{rt_name}/version/latest"
        assert len(self.runtime.list()) == 0
        self.runtime.build(workdir=rt_workdir)
        assert len(self.runtime.list()) == 1
        swrt = self.runtime.info(_rt_uri)
        assert swrt

        # 4.run evaluation on local instance
        logging.info("running evaluation at local...")
        assert len(self.evaluation.list()) == 0
        assert self.evaluation.run(
            model=_model_uri,
            datasets=[
                f'{ds_name}/version/{swds["version"]}',
                f'{ds_name}/version/{swds2["version"]}',
            ],
        )
        _eval_list = self.evaluation.list()
        assert len(_eval_list) == 1

        eval_info = self.evaluation.info(_eval_list[0]["manifest"]["version"])
        assert eval_info
        assert eval_info["manifest"]["status"] == "success"
        logging.info("finish run evaluation at standalone.")

        if mode != RunMode.SERVER:
            return
        # 5.login to server
        logging.info(f"login to server {server_uri} ...")
        assert self.instance.login(url=server_uri)

        # 6.copy local artifacts to server
        logging.info("copy local artifacts to server...")
        assert self.model.copy(
            src_uri=_model_uri,
            target_project=f"cloud://server/project/{server_project}",
            force=True,
        )
        assert self.dataset.copy(
            src_uri=f'{ds_name}/version/{swds["version"]}',
            target_project=f"cloud://server/project/{server_project}",
            force=True,
        )
        assert self.dataset.copy(
            src_uri=f'{ds_name}/version/{swds2["version"]}',
            target_project=f"cloud://server/project/{server_project}",
            force=True,
        )
        assert self.runtime.copy(
            src_uri=_rt_uri,
            target_project=f"cloud://server/project/{server_project}",
            force=True,
        )

        # 7.select to server instance
        self.instance.select(instance="server")
        self.project.select(project=server_project)

        _origin_job_list = self.evaluation.list(project=server_project)

        # 8.start an evaluation

        logging.info("running evaluation at server...")
        assert self.evaluation.run(
            model=swmp["version"],
            datasets=[swds["version"], swds2["version"]],
            runtime=swrt["version"],
            project=server_project,
            step_spec=step_spec_file,
            resource_pool=os.environ.get("RESOURCE_POOL"),
        )
        _new_job_list = self.evaluation.list(project=server_project)
        assert len(_new_job_list) == len(_origin_job_list) + 1

        _origin_job_ids = [j["manifest"]["id"] for j in _origin_job_list]
        _new_job_ids = [j["manifest"]["id"] for j in _new_job_list]

        _new_job_id = list(set(_new_job_ids) - set(_origin_job_ids))[0]

        # 9.check job's status
        _job_status = self.get_job_status(
            server_uri=server_uri, server_project=server_project, job_id=_new_job_id
        )

        while True:
            if _job_status == "SUCCESS" or _job_status == "FAIL":
                break
            sleep(10)
            _job_status = self.get_job_status(
                server_uri=server_uri, server_project=server_project, job_id=_new_job_id
            )
            if _job_status:
                logging.info(f"job status is:{_job_status}")
            else:
                logging.info("job status api occur some error!now will exit")
                break

        logging.info(f"finish run evaluation at server, status is:{_job_status}.")
        # 10.reset instance to local
        self.instance.select("local")

    def get_job_status(self, server_uri: str, server_project: str, job_id: str) -> Any:
        _remote_job = self.evaluation.info(
            f"{server_uri}/project/{server_project}/evaluation/{job_id}"
        )
        return _remote_job["manifest"]["jobStatus"] if _remote_job else "API ERROR"

    def test_mnist(self, server_url: Optional[str]) -> None:
        invoke(["cp", "-rf", f"{ROOT_DIR}/example", f"{self._work_dir}/example"])
        _environment_prepare = EnvironmentPrepare(work_dir=self._work_dir)
        _environment_prepare.prepare_mnist_data()
        _environment_prepare.prepare_mnist_requirements()

        self.standard_workflow(
            mode=RunMode.SERVER if server_url else RunMode.STANDALONE,
            model_name="mnist",
            model_workdir=f"{self._work_dir}/example/mnist",
            ds_name="mnist",
            ds_workdir=f"{self._work_dir}/example/mnist",
            rt_name="pytorch",
            rt_workdir=f"{self._work_dir}/example/runtime/pytorch",
            server_uri=server_url if server_url else "http://127.0.0.1:8082",
            server_project="starwhale",
            step_spec_file=f"{os.path.abspath(CURRENT_DIR)}/step_specs/step_spec_mnist_mini.yaml"
            if os.environ.get("GITHUB_ACTION")
            else f"{os.path.abspath(CURRENT_DIR)}/step_specs/step_spec_mnist_full.yaml",
        )

    def test_simple(self, server_url: Optional[str]) -> None:
        self.standard_workflow(
            mode=RunMode.SERVER if server_url else RunMode.STANDALONE,
            model_name="simple-test",
            model_workdir=f"{self._work_dir}/scripts/example",
            ds_name="simple-test",
            ds_workdir=f"{self._work_dir}/scripts/example",
            rt_name="simple-test",
            rt_workdir=f"{self._work_dir}/scripts/example",
            server_uri=server_url if server_url else "http://127.0.0.1:8082",
            server_project="starwhale",
            step_spec_file=f"{os.path.abspath(CURRENT_DIR)}/step_specs/step_spec_mnist_mini.yaml"
            if os.environ.get("GITHUB_ACTION")
            else f"{os.path.abspath(CURRENT_DIR)}/step_specs/step_spec_mnist_full.yaml",
        )

    # TODO add more example


def init_run_environment(work_dir: str) -> None:
    # prepare environment
    logging.info(f"work-dir is:{work_dir}")

    os.environ["SW_CLI_CONFIG"] = f"{work_dir}/config.yaml"
    os.environ["SW_LOCAL_STORAGE"] = f"{work_dir}/data"

    invoke(["cp", "-rf", f"{ROOT_DIR}/client", f"{work_dir}/client"])
    invoke(["cp", "-rf", f"{ROOT_DIR}/scripts", f"{work_dir}/scripts"])
    invoke(["cp", "-rf", f"{ROOT_DIR}/README.md", f"{work_dir}/README.md"])

    # install sw at current session
    logging.info(
        f"env PYPI_RELEASE_VERSION is:{os.environ.get('PYPI_RELEASE_VERSION')}"
    )
    invoke(["python3", "-m", "pip", "install", "-e", f"{work_dir}/client"])
    _res, _err = invoke(["swcli", "--version"])
    logging.info(f"pytest use swcli version is:{_res}")


class RunMode:
    STANDALONE = "standalone"
    SERVER = "server"


if __name__ == "__main__":
    with tempfile.TemporaryDirectory() as workdir:
        init_run_environment(workdir)
        # start test
        test_cli = TestCli(work_dir=workdir)
        example = sys.argv[1]
        _server_url = os.environ.get("CONTROLLER_URL")
        if example == "mnist":
            test_cli.test_mnist(_server_url)
        elif example == "simple":
            test_cli.test_simple(_server_url)
        else:
            logging.warning("there is nothing to run!")
