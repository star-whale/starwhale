import os
import tempfile

from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.base.invoke import invoke
from cmds.artifacts_cmd import Model, Dataset, Runtime
from cmds.eval_cmd import Evaluation

with tempfile.TemporaryDirectory() as _work_dir:
    _src_dir = os.environ.get("SW_SOURCE_DIR")
    print(f"tmp dir is {_work_dir}")
    # set runtime var
    os.environ['SW_CLI_CONFIG'] = f"{_work_dir}/config.yaml"
    os.environ['SW_LOCAL_STORAGE'] = f"{_work_dir}/data"
    # prepare test data
    invoke(["cp", "-rf", f"{_src_dir}/client", _work_dir])
    invoke(["cp", "-rf", f"{_src_dir}/example", _work_dir])
    invoke(["rm", "-rf", f"{_work_dir}/example/mnist/.venv"])
    invoke(["rm", "-rf", f"{_work_dir}/example/mnist/runtime"])
    invoke(["wget", "-P", f"{_work_dir}/example/mnist/data", "http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz"])
    invoke(["wget", "-P", f"{_work_dir}/example/mnist/data", "http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz"])
    invoke(["wget", "-P", f"{_work_dir}/example/mnist/data", "http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz"])
    invoke(["wget", "-P", f"{_work_dir}/example/mnist/data", "http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz"])

    invoke(["gzip", "-d", f"{_work_dir}/example/mnist/data/train-images-idx3-ubyte.gz"])
    invoke(["gzip", "-d", f"{_work_dir}/example/mnist/data/train-labels-idx1-ubyte.gz"])
    invoke(["gzip", "-d", f"{_work_dir}/example/mnist/data/t10k-images-idx3-ubyte.gz"])
    invoke(["gzip", "-d", f"{_work_dir}/example/mnist/data/t10k-labels-idx1-ubyte.gz"])

    # cloud login
    instance = Instance()
    res = instance.login()
    assert res
    print(f"login res:{res}")

    # 1.model build
    print("build model...")
    model = Model()
    assert len(model.list()) == 0
    assert model.build(workdir=f"{_work_dir}/example/mnist")
    assert len(model.list()) == 1
    swmp = model.info('mnist/version/latest')
    assert model.copy(src_uri='mnist/version/latest', target_project="cloud://cloud/project/starwhale", force=True)

    # 2.dataset build
    print("build dataset...")
    dataset = Dataset()
    assert len(dataset.list()) == 0
    assert dataset.build(workdir=f"{_work_dir}/example/mnist")
    assert len(dataset.list()) == 1
    swds = dataset.info('mnist/version/latest')
    assert dataset.copy(src_uri='mnist/version/latest',
                        target_project="cloud://cloud/project/starwhale",
                        with_auth=True,
                        force=True)

    # 3.runtime build
    print("build runtime...")
    rt = Runtime()
    assert len(rt.list()) == 0
    assert rt.build(workdir=f"{_work_dir}/example/runtime/pytorch")
    assert len(rt.list()) == 1
    swrt = rt.info('pytorch/version/latest')
    assert rt.copy(src_uri='pytorch/version/latest', target_project="cloud://cloud/project/starwhale", force=True)

    # use cloud instance
    instance.select("cloud")
    res = Project().select("starwhale")
    assert res
    print(f"project select self:{res}")

    # eval run
    print("run eval...")
    _eval = Evaluation()
    _origin_job_list = _eval.list(project="starwhale")
    assert len(_origin_job_list) != 0

    assert _eval.run(model=swmp["version"], dataset=swds["version"], runtime=swrt["version"], project="starwhale")
    _new_job_list = _eval.list(project="starwhale")
    assert len(_new_job_list) == len(_origin_job_list) + 1

    _origin_job_ids = [j["manifest"]["id"] for j in _origin_job_list]
    _new_job_ids = [j["manifest"]["id"] for j in _new_job_list]

    _new_job_id = list(set(_new_job_ids) - set(_origin_job_ids))

    print(f"eval info {_new_job_id[0]}:{_eval.info(f'http://console.pre.intra.starwhale.ai/project/starwhale/evaluation/{_new_job_id[0]}')}")

