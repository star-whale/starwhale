from pathlib import Path
import os
import io

import torch
from torchvision.transforms import functional as F
from PIL import Image
from starwhale.api.model import PipelineHandler
import numpy as nn

from . import model as mask_rcnn_model
from . import ds as penn_fudan_ped_ds
from . import coco_utils
from . import coco_eval

#import model as mask_rcnn_model
#import ds as penn_fudan_ped_ds
#import coco_utils
#import coco_eval

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_DTYPE_DICT_OUTPUT = {'boxes': torch.float32, 'labels': torch.int64, 'scores': torch.float32, 'masks': torch.uint8}
_DTYPE_DICT_LABEL = {'iscrowd': torch.int64, 'image_id': torch.int64, 'area': torch.float32, 'boxes': torch.float32, 'labels': torch.int64, 'scores': torch.float32, 'masks': torch.uint8}


class MARSKRCNN(PipelineHandler):

    def __init__(self, device="cuda") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)

    @torch.no_grad()
    def ppl(self, data, batch_size, **kw):
        model = self._load_model(self.device)
        image = Image.open(io.BytesIO(data))
        _image = F.to_tensor(image)
        outputs = model([_image.to(self.device)])
        cpu_device = torch.device("cpu")
        # [{'boxes':tensor[[],[]]},'labels':tensor[[],[]],'masks':tensor[[[]]]}]
        outputs = [{k: v.to(cpu_device) for k, v in t.items()} for t in outputs]
        for t in outputs:
            self.tensor_dict_to_list_dict(t)
            t['height'] = _image.shape[-2]
            t['width'] = _image.shape[-1]
        return outputs, None

    def handle_label(self, label, batch_size, **kw):
        image = Image.open(io.BytesIO(label))
        target = penn_fudan_ped_ds.mask_to_coco_target(image, kw['index'])
        return [self.tensor_dict_to_list_dict(target)]

    def list_dict_to_tensor_list(self, list_dict, label):
        for k in list_dict.keys():
            _value = list_dict.get(k)
            if isinstance(_value, list):
                list_dict[k] = torch.tensor(_value, dtype=_DTYPE_DICT_LABEL[k] if label else _DTYPE_DICT_OUTPUT[k])
        return list_dict


    def tensor_dict_to_list_dict(self,tensor_dict):
        for k in tensor_dict.keys():
            _value = tensor_dict.get(k)
            if isinstance(_value, torch.Tensor):
                tensor_dict[k] = _value.tolist()
        return tensor_dict

    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            _label.extend([self.list_dict_to_tensor_list(l, True) for l in _data["label"]])
            _result.extend([self.list_dict_to_tensor_list(r, False) for r in _data["result"]])
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


def load_test_env(fuse=True):
    _p = lambda p: str((_ROOT_DIR / "../test" / p).resolve())

    os.environ["SW_TASK_STATUS_DIR"] = "status/cmp"
    os.environ["SW_TASK_LOG_DIR"] = "log/cmp"
    os.environ["SW_TASK_RESULT_DIR"] = "result/cmp"

    # fname = "swds_fuse_simple.json" if fuse else "swds_s3_simple.json"
    os.environ["SW_TASK_INPUT_CONFIG"] = "../ppl_output.json"

def load_test_env_ppl(fuse=True):

    os.environ["SW_TASK_STATUS_DIR"] = "status"
    os.environ["SW_TASK_LOG_DIR"] = "log"
    os.environ["SW_TASK_RESULT_DIR"] = "result"

    # fname = "swds_fuse_simple.json" if fuse else "swds_s3_simple.json"
    os.environ["SW_TASK_INPUT_CONFIG"] = "/home/renyanda/.cache/starwhale/dataset/penn_fudan_ped/me2danlehfswknrrgfstcnlgmz4gs4q/local_fuse.json"


if __name__ == "__main__":
    load_test_env(fuse=True)
    mask_rcnn = MARSKRCNN()
    mask_rcnn._starwhale_internal_run_cmp()
