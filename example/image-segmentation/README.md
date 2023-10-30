Image Segmentation
======

Image segmentation is the process of partitioning a digital image into multiple image segments, also known as image regions or image objects (sets of pixels). The goal of segmentation is to simplify and/or change the representation of an image into something that is more meaningful and easier to analyze.

In these examples, we will use Starwhale to evaluate a set of image segmentation models.

What we learn
------

- use the `@Starwhale.predict` and `@Starwhale.evaluate` decorators to define handlers for Starwhale Model to finish the image segmentation evaluation tasks.
- build Starwhale Dataset by Starwhale Python SDK and use Starwhale Dataset Web Viewer.
- build Starwhale Dataset by `swcli dataset build -hf` command, no code, only one command.
- use Starwhale Evaluation Summary Page to compare the algorithm quality of the different models.
- build an unified Starwhale Runtime to run all models.

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

  - Introduction: The dataset contains 20 object categories including vehicles, household, animals and others. Each image in this dataset has pixel-level segmentation annotations, bounding box annotations, and object class annotations. This dataset has been widely used as a benchmark for object detection, semantic segmentation, and classification tasks.
  - Size: 1,464 images for training, 1,449 images for validation and a private testing set.

- [COCO-Stuff](https://github.com/nightrome/cocostuff)

  - Introduction: The Coco Stuff is dataset for scene understanding tasks like semantic segmentation, object detection and image captioning. It is constructed by annotating the original COCO dataset, which originally annotated things while neglecting stuff annotations.
  - Size: 164k images in COCO-stuff dataset that span over 172 categories including 80 things, 91 stuff, and 1 unlabeled class.

- [ADE20K](https://groups.csail.mit.edu/vision/datasets/ADE20K/)

  - Introduction: The ADE20K semantic segmentation dataset contains more than 20K scene-centric images exhaustively annotated with pixel-level objects and object parts labels. There are totally 150 semantic categories, which include stuffs like sky, road, grass, and discrete objects like person, car, bed.
  - Size: 25,574 for training and 2,000 for testing.
