Segment Anything meets Starwhale
======

The Segment Anything Model (SAM) produces high quality object masks from input prompts such as points or boxes, and it can be used to generate masks for all objects in an image. It has been trained on a dataset of 11 million images and 1.1 billion masks, and has strong zero-shot performance on a variety of segmentation tasks.

In this example, we use Starwhale to evaluate SAM, including online evaluation and offline evaluation.

Build Starwhale Model
------

- Prepare model checkpoints:

```bash
make download
```

- Build Starwhale Model:

```bash
swcli model build . -m sam --runtime image-segmentation --name sam-vit-b
```

Run Offline Evaluation in Standalone instance
------

```bash
# use source code
swcli -vvv model run -w . -m sam --dataset-head 10 --dataset coco2017-stuff-val

# use model package
swcli -vvv model run -u sam-vit-b --dataset coco2017-stuff-val --runtime image-segmentation
```

Run Online Evaluation in Standalone instance
------

```bash
# use source code
swcli -vvv model serve -w . -m sam

# use model package
swcli -vvv model server -u sam-vit-b
```

Visit <http://127.0.0.1:8080> to run online evaluation.

☕ Enjoy it.
