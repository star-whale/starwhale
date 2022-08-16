import io
import os
import pickle
from pathlib import Path

import torch
from PIL import Image
from torchvision.transforms import functional as F

from starwhale.api.model import PipelineHandler

from . import ds as penn_fudan_ped_ds
from . import model as mask_rcnn_model
from . import coco_eval, coco_utils

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_DTYPE_DICT_OUTPUT = {
    "boxes": torch.float32,
    "labels": torch.int64,
    "scores": torch.float32,
    "masks": torch.uint8,
}
_DTYPE_DICT_LABEL = {
    "iscrowd": torch.int64,
    "image_id": torch.int64,
    "area": torch.float32,
    "boxes": torch.float32,
    "labels": torch.int64,
    "scores": torch.float32,
    "masks": torch.uint8,
}


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
            output["height"] = _image.shape[-2]
            output["width"] = _image.shape[-1]
            _result.append(output)
        return _result

    def handle_label(self, label, **kw):
        files_bytes = pickle.loads(label)
        _result = []
        for idx, file_bytes in enumerate(files_bytes):
            image = Image.open(io.BytesIO(file_bytes.content_bytes))
            target = penn_fudan_ped_ds.mask_to_coco_target(image, kw["index"] + idx)
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
        coco_evaluator = coco_eval.CocoEvaluator(coco_ds, ["bbox", "segm"])
        for outputs, targets in zip(_result, _label):
            res = {targets["image_id"].item(): outputs}
            coco_evaluator.update(res)

        # gather the stats from all processes
        coco_evaluator.synchronize_between_processes()

        # accumulate predictions from all images
        coco_evaluator.accumulate()
        coco_evaluator.summarize()

        return [
            {
                iou_type: coco_eval.stats.tolist()
                for iou_type, coco_eval in coco_evaluator.coco_eval.items()
            }
        ]

    def _pre(self, input: bytes):
        image = Image.open(io.BytesIO(input))
        image = F.to_tensor(image)
        return [image.to(self.device)]

    def _load_model(self, device):
        s = _ROOT_DIR + "/models/maskrcnn.pth"
        net = mask_rcnn_model.get_model_instance_segmentation(2, False, torch.load(s))
        net = net.to(device)
        net.eval()
        print("mask rcnn model loaded, start to inference...")
        return net
