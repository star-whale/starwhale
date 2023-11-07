Image Classification
======

Image Classification is a fundamental task in vision recognition that aims to understand and categorize an image as a whole under a specific label.

In these examples, we will use Starwhale to evaluate a set of image classification models, the demos are in the [starwhale/image-classification](https://cloud.starwhale.cn/projects/399/overview) project of Starwhale Cloud.

What we learn
------

- use the `@starwhale.evaluation.predict` and `@starwhale.evaluation.evaluate` decorators to define handlers for Starwhale Model to finish the image classification evaluation tasks.
- use the `starwhale.PipelineHandler` class to define a model evaluation structure for image classification tasks.
- use Starwhale Online Evaluation.
- use the `@starwhale.multi_classification` decorator to simply the evaluation phase for the multi-classification problem.
- build Starwhale Dataset by Starwhale Python SDK and use Starwhale Dataset Web Viewer.
- build Starwhale Dataset by the one-line command from the Huggingface, no code.
- use one Starwhale Runtime to run all models.
- use `model.yaml` to define the Starwhale Model.

Models
------

- [Vision Transformer](https://paperswithcode.com/paper/an-image-is-worth-16x16-words-transformers-1) The Vision Transformer, or ViT, is a model for image classification that employs a Transformer-like architecture over patches of the image.
- [CNN](https://pytorch.org/tutorials/beginner/blitz/cifar10_tutorial.html): The Convolutional Neural Network (CNN or ConvNet) is a subtype of Neural Networks that is mainly used for applications in image and speech recognition.Its built-in convolutional layer reduces the high dimensionality of images without losing its information.

Datasets
------

- [CIFAR](https://www.cs.toronto.edu/~kriz/cifar.html): The CIFAR contains CIFAR-10 and CIFAR-100 datasets that are labeled subsets of the 80 million tiny images dataset.
