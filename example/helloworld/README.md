Starwhale Helloworld Example
======

This is the helloworld example of the Starwhale platform. It's the beginning for you to learn about the Starwhale platform. From this example, you can experience Starwhale's abstractions for model, dataset and runtime. You can also try out Starwhale's support for model evaluation and online scoring use cases.

The example does depend on GPUs and can run with just a few hundred MB of memory. You can go through the entire workflow in under 10 minutes.

The example uses KNN algorithm to classify image on MNIST(64 pixels) dataset.

Prerequisites
------

- Python 3.7 +
- Linux or macOS
- Starwhale Client 0.6.4+

Build Starwhale Runtime
------

```bash
swcli -vvv runtime build --yaml runtime.yaml
```

Build Starwhale Model
------

```bash
swcli -vvv model build . --name helloworld -m evaluation --runtime helloworld
```

Build Starwhale Dataset
------

```bash
swcli -vvv runtime activate helloworld
python3 dataset.py

#When you run `deactivate` command, the activated runtime environment will exit.
```

Run Evaluation in Standalone instance
------

```bash
# use code source to run
swcli -vvv model run -w . --dataset mnist64 -m evaluation --runtime helloworld

# use starwhale model to run
swcli -vvv model run --uri helloworld --dataset mnist64 --runtime helloworld
```

Enjoy it.
