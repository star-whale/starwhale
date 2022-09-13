import io
import os
import pickle

import torch
from PIL import Image
from torchvision.transforms import functional as F

from starwhale.api.job import Context
from starwhale.api.model import PipelineHandler

from . import model as mask_rcnn_model
from . import coco_eval, coco_utils

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class MARSKRCNN(PipelineHandler):
    def __init__(self, context: Context) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        super().__init__(context=context)

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

    def cmp(self, ppl_result):
        result, label = [], []
        for _data in ppl_result:
            label.append(_data["annotations"])
            (result) = _data["result"]
            result.extend(result)
        ds = zip(result, label)
        coco_ds = coco_utils.convert_to_coco_api(ds)
        coco_evaluator = coco_eval.CocoEvaluator(coco_ds, ["bbox", "segm"])
        for outputs, targets in zip(result, label):
            res = {targets["image_id"].item(): outputs}
            coco_evaluator.update(res)

        # gather the stats from all processes
        coco_evaluator.synchronize_between_processes()

        # accumulate predictions from all images
        coco_evaluator.accumulate()
        coco_evaluator.summarize()

        return {
            iou_type: coco_eval.stats.tolist()
            for iou_type, coco_eval in coco_evaluator.coco_eval.items()
        }

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
