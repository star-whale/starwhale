---
title: Object Detection & segmentation on PeenFudanPed dataset
---

This example will illustrate how to evaluate a pre-trained image object detection & segmentation model on StarWhale(`version:0.2.0b8`) under 6 steps

* Create a Runtime
* Train the model
* Implement the dataset slicing method
* Implement the inference method and evaluation metrics computing method
* Build Runtime, Model and Dataset
* Run the evaluation job and see the metrics

> :bulb: This example requires CUDA device

## Prerequisites

* Assume that you have Python3.7 or above installed.
* Clone starwhale repo

```shell
$ git clone https://github.com/star-whale/starwhale.git
$ cd starwhale/example/PennFudanPed
```

> :bulb: If you are from China mainland you're strongly recommended using a proxy

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

> The training code in this repo is sourced from https://pytorch.org/tutorials/intermediate/torchvision_tutorial.html However, some code is modified so that we could better understand how StarWhale works.

```shell
(visual_pytorch) $ mkdir models
(visual_pytorch) $ mkdir data
(visual_pytorch) $ cd data
(visual_pytorch) $ wget https://www.cis.upenn.edu/~jshi/ped_html/PennFudanPed.zip
(visual_pytorch) $ unzip PennFudanPed.zip
(visual_pytorch) $ cd ../code
(visual_pytorch) $ python train.py
```

You will get the logs below:

```shell
Epoch: [0]  [ 0/60]  eta: 0:00:53  lr: 0.000090  loss: 4.0966 (4.0966)  loss_classifier: 0.6816 (0.6816)  loss_box_reg: 0.5010 (0.5010)  loss_mask: 2.8732 (2.8732)  loss_objectness: 0.0377 (0.0377)  loss_rpn_box_reg: 0.0030 (0.0030)  time: 0.8854  data: 0.2439  max mem: 2301
...logs omitted...
creating index...
index created!
...logs omitted...
 Average Recall     (AR) @[ IoU=0.50:0.95 | area= large | maxDets=100 ] = 0.806
That's it!
```

Great! Now you have your model trained and saved. You could see it locates in the `models` directory.

## Slice the test dataset using Starwhale protocol

In the training section we use a dataset called [PennFudanPed](https://www.cis.upenn.edu/~jshi/ped_html/).

```shell
(visual_pytorch) $ ls ../data
PennFudanPed  PennFudanPed.zip
```

Before version `0.2.x` StarWhale will slice the dataset into chunks where reside the batched audios and batched labels. You need to tell StarWhale how to yield batches of byte arrays from files in the dataset.

In order to package images and labels in batch and convert them into byte array, we overwrite `iter_all_dataset_slice` and `iter_all_label_slice` method of parent class `BuildExecutor` in StarWhale sdk.
We package images' and labels' path into `FileBytes` which could make us easy to debug.

```python
class FileBytes:
    def __init__(self, p, byte_array):
        self.file_path = p
        self.content_bytes = byte_array


def _pickle_data(image_file_paths):
    all_bytes = [FileBytes(image_f, _image_to_bytes(image_f)) for image_f in
                 image_file_paths]
    return pickle.dumps(all_bytes)


def _pickle_label(label_file_paths):
    all_bytes = [FileBytes(label_f, _label_to_bytes(label_f)) for label_f in
                 label_file_paths]
    return pickle.dumps(all_bytes)


def _label_to_bytes(label_file_path):
    img = Image.open(label_file_path)
    img_byte_arr = io.BytesIO()
    img.save(img_byte_arr, format='PNG')
    return img_byte_arr.getvalue()


def _image_to_bytes(image_file_path):
    img = Image.open(image_file_path).convert("RGB")
    img_byte_arr = io.BytesIO()
    img.save(img_byte_arr, format='PNG')
    return img_byte_arr.getvalue()


class PennFudanPedSlicer(BuildExecutor):

    def iter_data_slice(self, path: str):
        pass

    def iter_label_slice(self, path: str):
        pass

    def iter_all_dataset_slice(self) -> t.Generator[t.Any, None, None]:
        datafiles = [p for p in self.iter_data_files()]
        idx = 0
        data_size = len(datafiles)
        while True:
            last_idx = idx
            idx = idx + self._batch
            if idx > data_size:
                break
            yield _pickle_data(datafiles[last_idx:idx])

    def iter_all_label_slice(self) -> t.Generator[t.Any, None, None]:
        labelfiles = [p for p in self.iter_label_files()]
        idx = 0
        data_size = len(labelfiles)
        while True:
            last_idx = idx
            idx = idx + self._batch
            if idx > data_size:
                break
            yield _pickle_label(labelfiles[last_idx:idx])

    def iter_data_slice(self, path: str):
        pass

    def iter_label_slice(self, path: str):
        pass
```

You need to extend the abstract class `BuildExecutor` so that your dataset could be used by StarWhale.

## Implement the inference method and evaluation metrics computing method

The inference method is called `ppl` and the evaluation metrics computing method is called `cmp`.
Here is the code snap from `ppl.py` where both methods are implemented. You need to extend the abstract class `PipelineHandler` so that you could receive the byte arrays you just transformed in last step.

There is a [flaw](https://github.com/star-whale/starwhale/issues/611) for the StarWhale(`version:0.2.0b8`) sdk that we must convert tensors to list(`tensor_dict_to_list_dict`) so that they could be serialized, and then convert list back to tensor(`list_dict_to_tensor_dict`). This is not that concise, because the framework hires jsonline to serialize python objects.

> :bulb: The reason why we serialize result of ppl instead of trigger cmp directly is that ppl and cmp are designed to be decoupled  phases. The ppl phase is designed to be executed on distributed machines which could reduce inference time significantly on large test dataset. So, there must be an inter-protocol between ppl and cmp.

```python
_DTYPE_DICT_OUTPUT = {'boxes': torch.float32, 'labels': torch.int64, 'scores': torch.float32, 'masks': torch.uint8}
_DTYPE_DICT_LABEL = {'iscrowd': torch.int64, 'image_id': torch.int64, 'area': torch.float32, 'boxes': torch.float32, 'labels': torch.int64, 'scores': torch.float32, 'masks': torch.uint8}


class MARSKRCNN(PipelineHandler):

    def __init__(self, device="cuda") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)

    @torch.no_grad()
    def ppl(self, data, batch_size, **kw):
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

    def handle_label(self, label, batch_size, **kw):
        files_bytes = pickle.loads(label)
        _result = []
        for idx, file_bytes in enumerate(files_bytes):
            image = Image.open(io.BytesIO(file_bytes.content_bytes))
            target = penn_fudan_ped_ds.mask_to_coco_target(image, kw['index'] * batch_size + idx)
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

    def _pre(self, input: bytes, batch_size: int):
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

StarWhale will feed the byte arrays of one batch to the `ppl` method. And take the output of `ppl` into a `inference_result` dict which looks like

```json
{"result":[{resultObj1},{resultObj2}],"label":[{labelObj1},{labelObj2}]}
```

StarWhale will automatically add result of `ppl` to `inference_result.result` and add result of `handle_label` to `inference_result.label`.

The `inference_result` is used in the argument of `cmp` which is named `_data_loader`.

### Implement cmp

`_data_loader` is an iterator for `result` and `label`. For a multiple classification problem, it is quite easy for you to implement the `cmp` method:

Just annotate your `cmp` method with `multi_classification` annotation and copy the lines inside it.

If you need to show `roc` and `auc`, you will also need to supply `_pr` in your `ppl` method.

By now we have finished all the coding part. Then let's begin the command line part.

## Build Runtime, Model and Dataset

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
There is some descriptive information needed for StarWhale to build a StarWhale Dataset(SWDS). The information is described by a yaml file like below:

```yaml
name: penn_fudan_ped
mode: generate
data_dir: data
data_filter: "PNGImages/*6.png"
label_filter: "PedMasks/*6_mask.png"
process: code.data_slicer:PennFudanPedSlicer

desc: PennFudanPed data and label test dataset
tag:
  - bin

attr:
    batch_size: 4
    alignment_size: 4k
    volume_size: 8M
```

Most of the fields are self-explained. The `process` descriptor is used to tell StarWhale that 'Hey, use SpeechCommandsSlicer to slice the dataset please!'.  Then StarWhale will use the files locate in `testing_list.txt` as input for `process`.

After create the yaml file under `${code_base}/example/PennFudanPed/`, we are ready to do it.

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

One step is left to success.

### Build Model

There is some descriptive information needed for StarWhale to build a StarWhale Model Package(SWMP). The information is described by a yaml file like below:

```yaml
version: 1.0
name: mask_rcnn
model:
  - models/maskrcnn.pth
run:
  ppl: code.ppl:MARSKRCNN
desc: mask rcnn resnet50 by pytorch
tag:
  - instance segmentation & object dectection
```

Most of the fields are self-explained. The `ppl` descriptor is used to tell StarWhale that 'Hey, run the inference method and cmp method with M5Inference please!'.
After create the yaml file under `${code_base}/example/PennFudanPed/`, we are ready to do it.

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

There we are. We have finished all the hard parts.

## Run the evaluation job and see the metrics

We have two ways to evaluate our model

* Evaluate model on local instance
* Evaluate model on cloud instance

### Evaluate model on local instance

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

### Evaluate model on cloud instance

* **Login on one cloud instance**

```shell
(visual_pytorch) $ swcli instance login http://console.pre.intra.starwhale.ai --username starwhale --password abcd1234 --alias pre-k8s
‍🍳 login http://console.pre.intra.starwhale.ai successfully!
```

* **Copy the model to cloud instance**

```shell
(visual_pytorch) $ swcli model copy mask_rcnn/version/mfstoolehayd cloud://pre-k8s/project/1
🚧 start to copy local/project/self/model/m5/version/mfstoolehayd -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload mfstoolehaydeyrvmyzdamzrmzshuma.swmp ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:07 94.0 MB 10.0 MB/s
👏 copy done.
```

* **Copy the dataset to cloud instance**

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

* **Copy the runtime to cloud instance**

```shell
(visual_pytorch) $ swcli runtime copy visual_pytorch/version/ga2wkmbwmizw cloud://pre-k8s/project/1
🚧 start to copy local/project/self/runtime/visual_pytorch/version/ga2wkmbwmizw -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload ga2wkmbwmizwkn3bmuytsmjunv3dc3q.swrt ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 20.5 kB ?
👏 copy done.
```

* **Go to the console and create one job**
