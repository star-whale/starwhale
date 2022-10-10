---
title: Object Detection & segmentation on PennFudanPed dataset
---

This example illustrates how to evaluate a pre-trained image object detection & segmentation model on Starwhale(`version:0.2.0b8`) in 6 steps

* Create a Runtime
* Train the model
* Implement the dataset slicing method
* Implement the inference method and evaluation metrics computing method
* Build Runtime, Model, and Dataset
* Run the evaluation job and look at the metrics

> :bulb: This example requires CUDA.

## Prerequisites

* Assume that you have Python3.7 or above installed.
* Clone the Starwhale repo

```shell
git clone https://github.com/star-whale/starwhale.git
cd starwhale/example/PennFudanPed
```

> :bulb: If you are from the mainland of China, we strongly recommend you use a proxy.

## Create a Runtime

```shell
$ swcli runtime create . --name visual_pytorch -m venv --python=3.8 --force
🍒 /home/renyanda/.config/starwhale/config.yaml use unexpected version(None), swcli only support 2.0 version.
🥕 /home/renyanda/.config/starwhale/config.yaml will be upgraded to 2.0 automatically.
🚧 start to create runtime environment...
👏 create venv@/home/renyanda/penn_fudan_ped/venv, python:3.8.10 (default, Mar 15 2022, 12:22:08)
[GCC 9.4.0]
🐶 install starwhale==0.2.0b8 venv@/home/renyanda/penn_fudan_ped/venv...
🍰 run command in shell 🍰
        source /home/renyanda/penn_fudan_ped/venv/bin/activate
👏 python runtime environment is ready to use 🎉
$ source ./venv/bin/activate
(visual_pytorch) $  python3 -m pip install -r requirements.txt
```

> :bulb: make sure python3.8-venv is installed if you choose --python=3.8
> :bulb: `python3 -m pip install` is recommended over `pip install`

## Train the model

> The training code in this repo is copied from [torchvision tutorial](https://pytorch.org/tutorials/intermediate/torchvision_tutorial.html). However, some code is modified to understand better how Starwhale works.

```shell
(visual_pytorch) $ mkdir models
(visual_pytorch) $ mkdir data
(visual_pytorch) $ cd data
(visual_pytorch) $ wget https://www.cis.upenn.edu/~jshi/ped_html/PennFudanPed.zip
(visual_pytorch) $ unzip PennFudanPed.zip
(visual_pytorch) $ cd ../code
(visual_pytorch) $ python train.py
```

You will get the logs as below:

```shell
Epoch: [0]  [ 0/60]  eta: 0:00:53  lr: 0.000090  loss: 4.0966 (4.0966)  loss_classifier: 0.6816 (0.6816)  loss_box_reg: 0.5010 (0.5010)  loss_mask: 2.8732 (2.8732)  loss_objectness: 0.0377 (0.0377)  loss_rpn_box_reg: 0.0030 (0.0030)  time: 0.8854  data: 0.2439  max mem: 2301
...logs omitted...
creating index...
index created!
...logs omitted...
 Average Recall     (AR) @[ IoU=0.50:0.95 | area= large | maxDets=100 ] = 0.806
That's it!
```

Great! Now, you have your model trained and saved. You can see it in the `models` directory.

## Slice the test dataset using the Starwhale protocol

In the training section, we use a dataset called [PennFudanPed](https://www.cis.upenn.edu/~jshi/ped_html/).

```shell
(visual_pytorch) $ ls ../data
PennFudanPed  PennFudanPed.zip
```

You need to extend the abstract class `BuildExecutor`, so Starwhale can use it.

## Implement the inference method and evaluation metrics computing method

The inference method is called `ppl`, and the evaluation metrics computing method is called `cmp`.
Here is the code snap from `ppl.py`, which implements both methods. You need to extend the abstract class `PipelineHandler` so you can receive the byte arrays, which you transformed in the last step.

There is a [flaw](https://github.com/star-whale/starwhale/issues/611) in the Starwhale(`version:0.2.0b8`) SDK. You must convert tensors to lists(`tensor_dict_to_list_dict`) in order to serialize them, and then convert them back(`list_dict_to_tensor_dict`). This is because the framework uses jsonline to serialize Python objects.

> :bulb: The reason we serialize ppl results instead of directly invoking cmp is that we design ppl and cmp as decoupled phases. We expect the ppl phase to be executed on distributed machines, which can significantly reduce inference time on large test datasets. So, there must be an inter-protocol between ppl and cmp.

```python
_DTYPE_DICT_OUTPUT = {'boxes': torch.float32, 'labels': torch.int64, 'scores': torch.float32, 'masks': torch.uint8}
_DTYPE_DICT_LABEL = {'iscrowd': torch.int64, 'image_id': torch.int64, 'area': torch.float32, 'boxes': torch.float32, 'labels': torch.int64, 'scores': torch.float32, 'masks': torch.uint8}


class MARSKRCNN(PipelineHandler):

    def __init__(self, device="cuda") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)

    @torch.no_grad()
    def ppl(self, data, **kw):
        model = self._load_model(self.device)
        files_bytes = pickle.loads(data)
        _result = []
        cpu_device = torch.device("cpu")
        for file_bytes in files_bytes:
            image = Image.open(io.BytesIO(file_bytes.content_bytes))
            _image = F.to_tensor(image)
            outputs = model([_image.to(self.device)])
            output = outputs[0]
            # [{'boxes':tensor[[],[]]},'labels':tensor[[],[]],'masks':tensor[[[]]]}]
            output = {k: v.to(cpu_device) for k, v in output.items()}
            output['height'] = _image.shape[-2]
            output['width'] = _image.shape[-1]
            _result.append(output)
        return _result

    def handle_label(self, label, **kw):
        files_bytes = pickle.loads(label)
        _result = []
        for idx, file_bytes in enumerate(files_bytes):
            image = Image.open(io.BytesIO(file_bytes.content_bytes))
            target = penn_fudan_ped_ds.mask_to_coco_target(image, kw['index'] + idx)
            _result.append(target)
        return _result

    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            # _label.extend([self.list_dict_to_tensor_dict(l, True) for l in _data[self._label_field]])
            _label.extend([l for l in _data[self._label_field]])
            (result) = _data[self._ppl_data_field]
            _result.extend(result)
        ds = zip(_result, _label)
        coco_ds = coco_utils.convert_to_coco_api(ds)
        coco_evaluator = coco_eval.CocoEvaluator(coco_ds,  ["bbox", "segm"])
        for outputs, targets in zip(_result, _label):
            res = {targets["image_id"].item(): outputs}
            coco_evaluator.update(res)

        # gather the stats from all processes
        coco_evaluator.synchronize_between_processes()

        # accumulate predictions from all images
        coco_evaluator.accumulate()
        coco_evaluator.summarize()

        return [{iou_type: coco_eval.stats.tolist() for iou_type, coco_eval in coco_evaluator.coco_eval.items()}]

    def _pre(self, input: bytes):
        image = Image.open(io.BytesIO(input))
        image = F.to_tensor(image)
        return [image.to(self.device)]

    def _load_model(self, device):
        s = _ROOT_DIR + "/models/maskrcnn.pth"
        net = mask_rcnn_model.get_model_instance_segmentation(2, False, torch.load(
            s))
        net = net.to(device)
        net.eval()
        print("mask rcnn model loaded, start to inference...")
        return net
```

### Implement ppl

Starwhale will feed the byte arrays of one batch to the `ppl` method and put the output of `ppl` into an `inference_result` dict, which looks like:

```json
{"result":[{resultObj1},{resultObj2}],"label":[{labelObj1},{labelObj2}]}
```

Starwhale will automatically add the result of `ppl` to `inference_result.result` and the result of `handle_label` to `inference_result.label`.

The `inference_result` is used in the argument of `cmp` named `_data_loader`.

### Implement cmp

`_data_loader` is an iterator for `result` and `label`. For a multiple classification problem, it is pretty easy to implement the `cmp` method by annotating your `cmp` method with the `multi_classification` annotation and coping the lines inside it.

If you need to show `roc` and `auc`, you will also need to supply `_pr` in your `ppl` method.

By now, we have finished all the coding parts. Then let's begin the command line part.

## Build Runtime, Model, and Dataset

### Build Runtime

```shell
(visual_pytorch) $ cd ..
(visual_pytorch) $ swcli runtime build .
🚧 start to build runtime bundle...
👷 uri:local/project/self/runtime/visual_pytorch
🐦 runtime will ignore pypi editable package
🆕 version gftdinztgqzt
📁 workdir: /home/renyanda/.cache/starwhale/self/workdir/runtime/visual_pytorch/gf/gftdinztgqztenddmvsdsolbnjxxgmy
💫 python3.8.10@venv, os(Linux), include-editable(False), try to export environment...
🌈 runtime docker image: ghcr.io/star-whale/starwhale:0.2.0b8  🌈
🦋 .swrt bundle:/home/renyanda/.cache/starwhale/self/runtime/visual_pytorch/gf/gftdinztgqztenddmvsdsolbnjxxgmy.swrt
  7 out of 7 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:00
```

### Build Dataset

Here is some descriptive information needed for Starwhale to build a Starwhale Dataset(SWDS). A yaml file describes the information as below:

```yaml
name: penn_fudan_ped
data_dir: data
data_filter: "PNGImages/*6.png"
label_filter: "PedMasks/*6_mask.png"
process: code.data_slicer:PennFudanPedSlicer

desc: PennFudanPed data and label test dataset
tag:
  - bin

attr:
    alignment_size: 4k
    volume_size: 8M
```

Most of the fields are self-explained. The `process` descriptor is the entry point of the data split method, and Starwhale will use the files in `testing_list.txt` as the input for `process`.

After creating the yaml file under `${code_base}/example/PennFudanPed/`, we are ready.

```shell
(visual_pytorch) $ swcli dataset build .
🚧 start to build dataset bundle...
👷 uri:local/project/self/dataset/penn_fudan_ped
🆕 version g5tggmbvhbsg
📁 swds workdir: /home/renyanda/.cache/starwhale/self/dataset/penn_fudan_ped/g5/g5tggmbvhbsgkzrwge4wizbvoj3xa3y.swds
👍 try to copy source code files...
🗣  swcli python prefix:/usr, runtime env python prefix:/home/renyanda/penn_fudan_ped/venv, swcli will inject sys.path
👻 import code.data_slicer:PennFudanPedSlicer@/home/renyanda/penn_fudan_ped to make swds...
cleanup done.
finish gen swds @ /home/renyanda/.cache/starwhale/self/dataset/penn_fudan_ped/g5/g5tggmbvhbsgkzrwge4wizbvoj3xa3y.swds/data
🤖 calculate signature...
🌺 congratulation! you can run  swcli dataset info penn_fudan_ped/version/g5tggmbvhbsgkzrwge4wizbvoj3xa3y
  8 out of 8 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:04
```

There is one more step left.

### Build Model

Here is some descriptive information for Starwhale to build a Starwhale Model Package(SWMP). A yaml file describes the information as below:

```yaml
version: 1.0
name: mask_rcnn
model:
  - models/maskrcnn.pth
run:
  ppl: code.ppl:MARSKRCNN
desc: mask rcnn resnet50 by PyTorch
tag:
  - instance segmentation & object dectection
```

Most of the fields are self-explained. The `ppl` descriptor is the entry point of the inference and cmp method.
After creating the yaml file under `${code_base}/example/PennFudanPed/`, we are ready.

```shell
(visual_pytorch) $ swcli model build .
🚧 start to build model bundle...
👷 uri:local/project/self/model/mask_rcnn
🆕 version mrrdoytdmq4d
📁 workdir: /home/renyanda/.cache/starwhale/self/workdir/model/mask_rcnn/mr/mrrdoytdmq4dqndcgu4dmntbonxhm2i
👍 try to copy source code files...
🦋 .swmp bundle:/home/renyanda/.cache/starwhale/self/model/mask_rcnn/mr/mrrdoytdmq4dqndcgu4dmntbonxhm2i.swmp
  6 out of 6 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:03
```

Here we are. We have finished all the complex parts.

## Run the evaluation job and see the metrics

We have two ways to evaluate our model.

* Evaluate the model on the local standalone instance
* Evaluate the model on a cloud instance

### Evaluate the model on the local standalone instance

#### Create the job

```shell
$ swcli job create self --model mask_rcnn/version/latest --dataset penn_fudan_ped/version/latest --runtime visual_pytorch/version/latest
😹 /home/renyanda/.cache/starwhale/self/workdir/model/mask_rcnn/mr/mrrdoytdmq4dqndcgu4dmntbonxhm2i existed, skip extract model bundle
👏 render swds penn_fudan_ped:g5tggmbvhbsgkzrwge4wizbvoj3xa3y local_fuse.json
🔍 /home/renyanda/.cache/starwhale/self/dataset/penn_fudan_ped/g5/g5tggmbvhbsgkzrwge4wizbvoj3xa3y.swds/local_fuse.json
try to import code.ppl:MARSKRCNN@/home/renyanda/.cache/starwhale/self/workdir/model/mask_rcnn/mr/mrrdoytdmq4dqndcgu4dmntbonxhm2i/src...
🗣  swcli python prefix:/usr, runtime env python prefix:/home/renyanda/penn_fudan_ped/venv, swcli will inject sys.path
mask rcnn model loaded, start to inference...
mask rcnn model loaded, start to inference...
mask rcnn model loaded, start to inference...
mask rcnn model loaded, start to inference...
👏 finish run ppl: PipelineHandler status@/home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/ppl/status, log@/home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/ppl/log, result@/home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/ppl/result
try to import code.ppl:MARSKRCNN@/home/renyanda/.cache/starwhale/self/workdir/model/mask_rcnn/mr/mrrdoytdmq4dqndcgu4dmntbonxhm2i/src...
🗣  swcli python prefix:/usr, runtime env python prefix:/home/renyanda/penn_fudan_ped/venv, swcli will inject sys.path
creating index...
index created!
Accumulating evaluation results...
DONE (t=0.00s).
Accumulating evaluation results...
DONE (t=0.00s).
IoU metric: bbox
 Average Precision  (AP) @[ IoU=0.50:0.95 | area=   all | maxDets=100 ] = 0.260
IoU metric: segm
 Average Precision  (AP) @[ IoU=0.50:0.95 | area=   all | maxDets=100 ] = 0.000
👏 finish run cmp: PipelineHandler status@/home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/cmp/status, log@/home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/cmp/log, result@/home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/cmp/result
  7 out of 7 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:01:11
👏 success to create job(project id: local/project/self)
🐦 run cmd to fetch job info: swcli job info guzgeztdga4t
```

#### See the metrics

```shell
(visual_pytorch) $ cat /home/renyanda/.cache/starwhale/self/job/gu/guzgeztdga4tqzjzmvqtenjvnvstkyi/cmp/result/current
[{"bbox": [0.25974458315396753, 0.31819486296455735, 0.26732673267326734, -1.0, 0.3029702970297029, 0.2615275813295615, 0.08666666666666667, 0.2644444444444444, 0.2644444444444444, -1.0, 0.3, 0.2619047619047619], "segm": [0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0]}]
```

Congratulations, we have nearly finished the whole example! From now on, we can update the training method, get a new model, build a new SWMP and evaluate our model from time to time.

### Evaluate model on a cloud instance

* **Log in to a cloud instance**

```shell
(visual_pytorch) $ swcli instance login http://console.pre.intra.starwhale.ai --username starwhale --password abcd1234 --alias pre-k8s
‍🍳 login http://console.pre.intra.starwhale.ai successfully!
```

* **Copy the model to the cloud instance**

```shell
(visual_pytorch) $ swcli model copy mask_rcnn/version/mfstoolehayd cloud://pre-k8s/project/1
🚧 start to copy local/project/self/model/m5/version/mfstoolehayd -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload mfstoolehaydeyrvmyzdamzrmzshuma.swmp ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:07 94.0 MB 10.0 MB/s
👏 copy done.
```

* **Copy the dataset to the cloud instance**

```shell
(visual_pytorch) $ swcli dataset copy penn_fudan_ped/version/gmzgczrqmezd cloud://pre-k8s/project/1
🚧 start to copy local/project/self/dataset/speechcommands/version/gmzgczrqmezd -> http://console.pre.intra.starwhale.ai/project/1...
  ⬆ _manifest.yaml         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 4.3 kB  ?
  ⬆ data_ubyte_0.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:05 72.3 MB 9.6 MB/s
  ⬆ data_ubyte_1.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:13 72.8 MB 9.7 MB/s
  ⬆ data_ubyte_2.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:21 72.6 MB 9.7 MB/s
  ⬆ data_ubyte_3.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:29 72.6 MB 9.7 MB/s
  ⬆ data_ubyte_4.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:37 73.0 MB 9.8 MB/s
  ⬆ data_ubyte_5.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:45 72.8 MB 9.4 MB/s
  ⬆ data_ubyte_6.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:53 72.8 MB 9.6 MB/s
  ⬆ data_ubyte_7.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:01 72.7 MB 9.7 MB/s
  ⬆ data_ubyte_8.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:09 72.5 MB 9.7 MB/s
  ⬆ data_ubyte_9.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:13 32.5 MB 6.9 MB/s
  ⬆ index.jsonl            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 15.2 kB ?
  ⬆ label_ubyte_0.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_1.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_2.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_3.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_4.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_5.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_6.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_7.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_8.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 36.6 kB ?
  ⬆ label_ubyte_9.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:15 16.3 kB ?
  ⬆ archive.swds_meta      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:01:23 93.9 MB 10.0 MB/s
👏 copy done
```

* **Copy the runtime to the cloud instance**

```shell
(visual_pytorch) $ swcli runtime copy visual_pytorch/version/ga2wkmbwmizw cloud://pre-k8s/project/1
🚧 start to copy local/project/self/runtime/visual_pytorch/version/ga2wkmbwmizw -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload ga2wkmbwmizwkn3bmuytsmjunv3dc3q.swrt ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 20.5 kB ?
👏 copy done.
```

* **Go to the console and create one job**
