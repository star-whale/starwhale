YOLO meets Starwhale
======

YOLO (You Only Look Once) is a target detection algorithm based on deep learning, proposed by Redmon et al. in 2016. Its main features are high speed and accuracy. Through continuous improvement, the YOLO algorithm currently has iterated to the 8th version.

In this example, we use [ultralytics](https://github.com/ultralytics/ultralytics) lib to evaluate YOLOv8 and YOLOv5 models.

Build Starwhale Model
------

```bash
swcli runtime activate object-detection
# build yolov8n model
python3 build.py yolov8n
# build all YOLOv8 and YOLOv5 models
python3 build.py all
```

Run Offline Evaluation in Standalone instance
------

```bash
# use source code
swcli -vvv model run -w . -m evaluation --handler evaluation:summary_detection --dataset coco128 --dataset-head 4 --runtime object-detection

# use Starwhale Model
swcli -vvv model run -u yolov8n --handler evaluation:summary_detection --dataset coco128 --runtime object-detection
```

Run Online Evaluation in Standalone instance
---

```bash
swcli runtime activate object-detection
swcli -vvv model serve -w . -m evaluation
```

Then visit <http://127.0.0.1:8080>.
