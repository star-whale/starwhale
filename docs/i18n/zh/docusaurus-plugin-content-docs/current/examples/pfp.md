---
title: PennFudanPed数据集的目标检测任务模型评测
---

本例子[参考TorchVision](https://pytorch.org/tutorials/intermediate/torchvision_tutorial.html)对[PennFudanPed数据集](https://www.cis.upenn.edu/~jshi/ped_html/)进行Neural Machine Translation和模型评测，相关代码的链接：[example/PennFudanPed](https://github.com/star-whale/starwhale/tree/main/example/PennFudanPed)。

从该例中，我们能实践如下Starwhale功能：

- 如何使用COCOObjectAnnotation类型作为Annotation构建适用于目标检测的数据集。
- 如何使用 `starwhale.Evaluation` SDK自主上报评测结果。

## 前置条件

阅读本文前，建议先阅读[Pytorch Runtime构建](../runtime/examples/pytorch.md), [Speech Commands数据集的多分类任务模型评测](speech.md)。

### 基础环境

- Python版本: 3.7 ~ 3.10。
- OS环境: Linux或macOS(仅运行Standalone)。
- Starwhale Client 完成安装，且版本不早于0.3.0。
- [可选]Starwhale Controller 完成安装，且版本不早于0.3.0，如果只希望在Standalone Instance中进行评测，可以忽略该步骤。
- Runtime: [Pytorch Runtime Example](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)

### Starwhale Runtime激活

本例可以使用Starwhale提供的[Pytorch Runtime例子](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)作为Starwhale Runtime，不需要额外编写Runtime配置。模型训练和评测都可以使用该Runtime。

- 准备Runtime：下载或重新构建，如何Standalone Instance中已经有该Runtime，则忽略该步骤。

```shell
# 下载Starwhale Cloud已经构建好的Pytorch Runtime
swcli runtime copy https://cloud.starwhale.ai/project/demo/runtime/pytorch/version/latest self

# 或根据pytorch runtime example在本地构建一个新的Runtime
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/runtime/pytorch
swcli runtime build .
```

- Activate Runtime：在当前shell中激活相应的Python环境

```shell
swcli runtime activate pytorch/version/latest
```

### 数据准备与模型训练

数据准备和模型训练非常容易，只需要三步就能完成操作：下载代码、下载数据、开始训练。

```shell
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/PennFudanPed
make download-data
make train
```

- `make download-data` 命令下载数据的时候，如果遇到网络问题，请合理设置代理。
- `make train` 命令需要在Pytorch Runtime 已经激活的Shell环境中执行，否则可能提示某些Python包Import Error。
- `make train` 过程可能会比较慢，执行时间长短取决于机器配置、GPU资源情况等。
- 命令执行结束后，可以到`data`目录查看原始数据，`models`目录查看已经构建好的模型。
- 可以在train.py对训练过程的一些参数进行调整，比如epoch值等。

## Starwhale的模型评测过程

### 步骤1：构建Starwhale Dataset

```bash
# 如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate pytorch/version/latest
# 根据dataset.yaml构建swds-bin格式in格式的数据集
swcli dataset build .
# 查看最新构建的数据集详情
swcli dataset info pfp/version/latest
```

上面的`build`命令在`starwhale/example/PennFudanPed`中执行，也可以在其他目录中执行，但要合理设置 `swcli dataset build`命令的`WORKDIR`参数。除了可以在执行build命令前执行 `runtime activate` 命令激活Runtime，也可以在 `model build` 命令中添加 `--runtime pytorch/version/latest` 参数，确保执行该命令是在Pytorch Runtime中进行的。

PennFudanPed 例子是比较典型的COCO格式数据集形式，Starwhale SDK提供 `COCOObjectAnnotation` 类型和多Annotation机制，可以让数据集构建非常容易，核心代码如下：

```python
from PIL import Image as PILImage
from pycocotools import mask as coco_mask
from starwhale import Image, MIMEType, BoundingBox, COCOObjectAnnotation

class PFPDatasetBuildExecutor:
    def __iter__(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data" / "PennFudanPed"
        names = [p.stem for p in (root_dir / "PNGImages").iterdir()]
        self.object_id = 1
        for idx, name in enumerate(sorted(names)):
            data_fpath = root_dir / "PNGImages" / f"{name}.png"
            mask_fpath = root_dir / "PedMasks" / f"{name}_mask.png"
            height, width = self._get_image_shape(data_fpath)
            coco_annotations = self._make_coco_annotations(mask_fpath, idx)
            annotations = {
                "mask": Image(
                    mask_fpath,
                    display_name=name,
                    mime_type=MIMEType.PNG,
                    shape=(height, width, 3),
                    as_mask=True,
                    mask_uri=name,
                ).carry_raw_data(),
                "image": {"id": idx, "height": height, "width": width, "name": name},
                "object_nums": len(coco_annotations),
                "annotations": coco_annotations,
            }
            data = Image(
                data_fpath,
                display_name=name,
                mime_type=MIMEType.PNG,
                shape=(height, width, 3),
            )
            yield data, annotations

    def _make_coco_annotations(
        self, mask_fpath: Path, image_id: int
    ) -> t.List[COCOObjectAnnotation]:
        mask_img = PILImage.open(str(mask_fpath))

        mask = np.array(mask_img)
        object_ids = np.unique(mask)[1:]
        binary_mask = mask == object_ids[:, None, None]
        # TODO: tune permute without pytorch
        binary_mask_tensor = torch.as_tensor(binary_mask, dtype=torch.uint8)
        binary_mask_tensor = (
            binary_mask_tensor.permute(0, 2, 1).contiguous().permute(0, 2, 1)
        )

        coco_annotations = []
        for i in range(0, len(object_ids)):
            _pos = np.where(binary_mask[i])
            _xmin, _ymin = float(np.min(_pos[1])), float(np.min(_pos[0]))
            _xmax, _ymax = float(np.max(_pos[1])), float(np.max(_pos[0]))
            _bbox = BoundingBox(
                x=_xmin, y=_ymin, width=_xmax - _xmin, height=_ymax - _ymin
            )

            rle: t.Dict = coco_mask.encode(binary_mask_tensor[i].numpy())  # type: ignore
            rle["counts"] = rle["counts"].decode("utf-8")

            coco_annotations.append(
                COCOObjectAnnotation(
                    id=self.object_id,
                    image_id=image_id,
                    category_id=1,  # PennFudan Dataset only has one class-PASPersonStanding
                    segmentation=rle,
                    area=_bbox.width * _bbox.height,
                    bbox=_bbox,
                    iscrowd=0,  # suppose all instances are not crowd
                )
            )
            self.object_id += 1

        return coco_annotations
```

同时Cloud Instance对此类数据集提供了适合的可视化呈现方式。

![pfp-dataset.gif](../img/examples/pfp-dataset.gif)

### 步骤2：Standalone Instance中评测模型

```bash
# 根据model.yaml运行评测任务
swcli model eval . --dataset mask_rcnn/version/latest
# 展示评测结果
swcli model info ${version}
```

上面的`build`命令在`starwhale/example/PennFudanPed`中执行，也可以在其他目录中执行，但要合理设置 `swcli model eval`命令的`WORKDIR`参数。

PennFudanPed例子是多目标检测问题，无法使用 `starwhale.multi_classification` 修饰器，Starwhale SDK中也没有提供合适的修饰器自动处理cmp结果。本例中，我们使用 `self.evaluation_store.log_metrics` 函数，将report的结果存储到Starwhale Datastore中，这样在Standalone Instance 和 Cloud Instance中都能看到相关结果。用户可以使用 `evaluation` SDK上报各种评测结果数据。

cmp中核心代码：

```python
def cmp(self, ppl_result):
    pred_results, annotations = [], []
    for _data in ppl_result:
        annotations.append(_data["input"])
        pred_results.append(_data["output"])

    evaluator = make_coco_evaluator(annotations, iou_types=self.iou_types)
    for index, pred in pred_results:
        evaluator.update({index: pred})

    evaluator.synchronize_between_processes()
    evaluator.accumulate()
    evaluator.summarize()

    detector_metrics_map = [
        "average_precision",
        "average_precision_iou50",
        "average_precision_iou75",
        "ap_across_scales_small",
        "ap_across_scales_medium",
        "ap_across_scales_large",
        "average_recall_max1",
        "average_recall_max10",
        "average_recall_max100",
        "ar_across_scales_small",
        "ar_across_scales_medium",
        "ar_across_scales_large",
    ]

    report = {"kind": "coco_object_detection", "bbox": {}, "segm": {}}
    for _iou, _eval in evaluator.coco_eval.items():
        if _iou not in report:
            continue

        _stats = _eval.stats.tolist()
        for _idx, _label in enumerate(detector_metrics_map):
            report[_iou][_label] = _stats[_idx]

    self.evaluation_store.log_metrics(report)
```

在Standalone Instance中呈现评测结果：

![eval-info.png](../img/examples/pfp-eval-info.png)

在Cloud Instance中呈现评测结果：

![eval-cloud.png](../img/examples/pfp-cloud-result-diff.png)

### 步骤3：构建Starwhale Model

一般情况下，用户经过多次运行模型评测命令(步骤2)进行调试，得到一个可以在大数据量下运行评测或可发布的模型，就需要执行步骤3，构建一个可分发的Starwhale Model。

```shell
#根据model.yaml构建Starwhale Model
swcli model build .
# 查看最新构建的模型信息
swcli model info mask_rcnn/version/latest
```

### 步骤4：Cloud Instance中评测模型（可选）

在Cloud Instance上运行评测任务，需要将Standalone Instance上构建的Model、Dataset和Runtime发布到相应的Instance上。

```shell
# 登陆相关instance，之后可以用 prod alias访问该instance
swcli instance login --username ${username} --token ${token}  http://${instance-address} --alias prod
# 将本地默认instance改为standalone
swcli instance select local
#上传model到prod instance中name为starwhale的project中
swcli model copy mask_rcnn/version/latest cloud://prod/project/starwhale
#上传dataset到prod instance中name为starwhale的project中
swcli dataset copy pfp/version/latest cloud://prod/project/starwhale
#上传runtime到prod instance中name为starwhale的project中
swcli runtime copy pytorch/version/latest cloud://prod/project/starwhale
```

然后，可以在终端中执行`swcli ui prod`命令，可以拉起浏览器并进入prod instance的web页面中，接着进入相关project，创建评测任务即可。

## 参考资料

- [Penn-Fudan Database for Pedestrian Detection and Segmentation](https://www.cis.upenn.edu/~jshi/ped_html/)
- [TorchVision object detection finetuning tutorial](https://pytorch.org/tutorials/intermediate/torchvision_tutorial.html)
