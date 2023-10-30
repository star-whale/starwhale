Image Segmentation
======

Image segmentation is the process of partitioning a digital image into multiple image segments, also known as image regions or image objects (sets of pixels). The goal of segmentation is to simplify and/or change the representation of an image into something that is more meaningful and easier to analyze.

In these examples, we will use Starwhale to evaluate a set of image segmentation models.

What we learn
------

- use the `@Starwhale.predict` and `@Starwhale.evaluate` decorators to define handlers for Starwhale Model to finish the image segmentation evaluation tasks.
- build Starwhale Dataset by Starwhale Python SDK and use Starwhale Dataset Web Viewer.
- use Starwhale Evaluation Summary Page to compare the algorithm quality of the different models.
- build an unified Starwhale Runtime to run all models.
- use Starwhale `replicas` feature to speedup model evaluation.
- use `Starwhale.Image` and `Starwhale.COCOObjectAnnotation` to represent Dataset type.

Models
------

- [Segment Anything Model](https://segment-anything.com/)
- [GrabCut](https://docs.opencv.org/3.4/d8/d83/tutorial_py_grabcut.html)
- [PSPNet](https://github.com/qubvel/segmentation_models)
- [FCN](https://pytorch.org/vision/main/models/fcn.html)
- [SAN](https://github.com/MendelXu/SAN)

Datasets
------

We will use the following datasets to evaluate models.

- [PASCAL VOC](http://host.robots.ox.ac.uk/pascal/VOC/)

  - Introduction: The dataset contains 20 object categories including vehicles, household, animals and others. Each image in this dataset has pixel-level segmentation annotations, bounding box annotations, and object class annotations. This dataset has been widely used as a benchmark for object detection, semantic segmentation, and classification tasks. In this example, we will use 2012 year dataset.
  - Size: The train/val data has 11,530 images containing 27,450 ROI annotated objects and 6,929 segmentations.

- [COCO 2017 Stuff](https://cocodataset.org/#stuff-2017)

  - Introduction: The COCO Stuff Segmentation Task is designed to push the state of the art in semantic segmentation of stuff classes. Whereas the object detection task addresses thing classes (person, car, elephant), this task focuses on stuff classes (grass, wall, sky).
  - Size: Validation images 5,000.

- [ADE20K](https://groups.csail.mit.edu/vision/datasets/ADE20K/)

  - Introduction: The ADE20K semantic segmentation dataset contains more than 20K scene-centric images exhaustively annotated with pixel-level objects and object parts labels. There are totally 150 semantic categories, which include stuffs like sky, road, grass, and discrete objects like person, car, bed.
  - Size: 25,574 for training and 2,000 for testing.
