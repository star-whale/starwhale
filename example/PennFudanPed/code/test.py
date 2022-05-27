from pathlib import Path
import os

import torch
from model import get_model_instance_segmentation
from ds import PennFudanDataset
from train import get_transform
import utils
import coco_utils
import coco_eval

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_MODEL_PATH = os.path.join(_ROOT_DIR, "../models/maskrcnn.pth")
_DATA_PATH = os.path.join(_ROOT_DIR, "../data/PennFudanPed")


def _load_model( device):
    model = get_model_instance_segmentation(2, False, torch.load(_MODEL_PATH))
    model = model.to(device)
    model.eval()
    print("mask rcnn model loaded, start to inference...")
    return model


@torch.no_grad()
def main():
    dataset_test = PennFudanDataset(_DATA_PATH, get_transform(train=False))
    indices = torch.randperm(len(dataset_test)).tolist()
    dataset_test = torch.utils.data.Subset(dataset_test, indices[-1:])
    data_loader_test = torch.utils.data.DataLoader(
        dataset_test, batch_size=1, shuffle=False, num_workers=4,
        collate_fn=utils.collate_fn)
    device = torch.device('cuda')
    model = _load_model(device)
    cpu_device = torch.device("cpu")
    coco = coco_utils.get_coco_api_from_dataset(data_loader_test.dataset)
    coco_evaluator = coco_eval.CocoEvaluator(coco, ["bbox", "segm"])
    for images, targets in data_loader_test:
        images = list(img.to(device) for img in images)
        torch.cuda.synchronize()
        outputs = model(images)
        outputs = [{k: v.to(cpu_device) for k, v in t.items()} for t in outputs]
        res = {target["image_id"].item(): output for target, output in
               zip(targets, outputs)}
        print(res)
        coco_evaluator.update(res)

    # gather the stats from all processes
    coco_evaluator.synchronize_between_processes()

    # accumulate predictions from all images
    coco_evaluator.accumulate()
    coco_evaluator.summarize()
    result = [{iou_type: coco_eval.stats for iou_type, coco_eval in coco_evaluator.coco_eval.items()}]
    print(result)

if __name__ == "__main__":
    main()
