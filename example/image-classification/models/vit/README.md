Vision Transformer
======

- 🏔️ Homepage: <https://arxiv.org/abs/2010.11929>
- 🌋 Github: <https://github.com/google-research/vision_transformer>
- 🤗 HuggingFace: <https://huggingface.co/aaraki/vit-base-patch16-224-in21k-finetuned-cifar10>
- 🏕️ Size: 86.6M
- 🎇 Introduction: Vision Transformer (ViT) model pre-trained on ImageNet-21k (14 million images, 21,843 classes) at resolution 224x224, and fine-tuned on ImageNet 2012 (1 million images, 1,000 classes) at resolution 224x224. This model is a fine-tuned of ViT base model on CIFAR10 dataset.

In this example, we will use fine-tuned ViT model from transformers lib to classify CIFAR10 and CIFAR100 datasets.

Building Starwhale Model
------

```bash
python3 build.py
```

Running Online Evaluation in Standalone Instance
------

```bash
# use source code
swcli -vvv model serve --workdir . -m inference --runtime image-classification

# use starwhale model package
swcli -vvv model serve --uri vit-finetuned-cifar10 --runtime image-classification
```

Then you can visit <http://127.0.0.1:8080> to upload image for classification.


Running Evaluation in Standalone Instance
------

```bash
# use source code to evaluate on cifar10 dataset
swcli -vvv model run -w . -m inference --runtime image-classification --dataset cifar10 --handler inference:reduce_evaluate

# use starwhale model package to evaluate on cifar10 dataset
swcli -vvv model run -u vit-finetuned-cifar10 --runtime image-classification --dataset cifar10 --handler inference:reduce_evaluate
```
