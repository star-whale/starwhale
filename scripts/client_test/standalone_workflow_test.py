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

    # use local instance
    instance.select("local")
    res = Project().select("self")
    assert res
    print(f"project select self:{res}")

    # 1.model build
    print("build model...")
    model = Model()
    assert len(model.list()) == 0
    assert model.build(workdir=f"{_work_dir}/example/mnist")
    assert len(model.list()) == 1
    print(f"swmp info:{model.info('mnist/version/latest')}")

    # 2.dataset build
    print("build dataset...")
    dataset = Dataset()
    assert len(dataset.list()) == 0
    assert dataset.build(workdir=f"{_work_dir}/example/mnist")
    assert len(dataset.list()) == 1
    print(f"swds info:{dataset.info('mnist/version/latest')}")

    # 3.runtime build
    print("build runtime...")
    rt = Runtime()
    assert len(rt.list()) == 0
    assert rt.build(workdir=f"{_work_dir}/example/runtime/pytorch")
    assert len(rt.list()) == 1
    print(f"swrt info:{rt.info('pytorch/version/latest')}")

    # 4.eval run
    print("run eval...")
    _eval = Evaluation()
    assert len(_eval.list()) == 0
    assert _eval.run(model="mnist/version/latest", dataset="mnist/version/latest")
    _eval_list = _eval.list()
    assert len(_eval_list) == 1

    print(f"eval info:{_eval.info(_eval_list[0]['manifest']['version'])}")

    res, err = invoke(["ls", "-l", _work_dir])
    print(f"workdir is {res}")

