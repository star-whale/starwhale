Object Detection
======

Object detection is a computer vision technique for locating instances of objects in images or videos. Object detection algorithms typically leverage machine learning or deep learning to produce meaningful results.

In these examples, we will use Starwhale to evaluate a set of object detection models on COCO datasets.

Thanks to [ultralytics](https://github.com/ultralytics/ultralytics), it makes Starwhale Model Evaluation on YOLO easily.

Links
------

- Github Example Code: <https://github.com/star-whale/starwhale/tree/main/example/object-detection>
- Starwhale Cloud Demo: <https://cloud.starwhale.cn/projects/397/overview>

What we learn
------

- build Starwhale Dataset by Starwhale Python SDK and use Starwhale Dataset Web Viewer.

Models
------

- [YOLO](https://docs.ultralytics.com/): We will compare YOLOv8-{n,s,m,l,x} and YOLOv6-{n,s,m,l,l6} model evaluations.

Datasets
------

- [COCO128](https://github.com/ultralytics/ultralytics/blob/main/ultralytics/cfg/datasets/coco128.yaml)

  - Introduction: Ultralytics COCO8 is a small, but versatile object detection dataset composed of the first 128 images of the COCO train 2017 set. This dataset is ideal for testing and debugging object detection models.
  - Size: Validation images 128.
  - Dataset build command:

    ```bash
    swcli runtime activate object-detection
    python3 datasets/coco128.py
    ```

- [COCO_val2017](https://cocodataset.org/#download)

  - Introduction: The COCO (Common Objects in Context) dataset is a large-scale object detection, segmentation, and captioning dataset. It is designed to encourage research on a wide variety of object categories and is commonly used for benchmarking computer vision models. The dataset comprises 80 object categories.
  - Size: Validation images 5,000.
  - Dataset build command:

    ```bash
    swcli runtime activate object-detection
    python3 datasets/coco_val2017.py
    ```
