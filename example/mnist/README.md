MNIST Example
-----------------

# How to train?
cmd: make train
output: models/mnist_cnn.pt, which is pre-trained model.

# How to test?
cmd: make test
output: in stdout, we will get loss result.

# How to inference?
cmd: make inference


Ref
--------
- https://github.com/pytorch/examples/blob/main/mnist/main.py
- https://nextjournal.com/gkoehler/pytorch-mnist