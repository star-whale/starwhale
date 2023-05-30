.POHNY: train
train:
	python mnist/train.py

.POHNY: download-model
download-model:
	[ -d models ] ||  mkdir -p models
	[ -f models/mnist_cnn.pt ] || ([ -z $(CN) ] && curl -o models/mnist_cnn.pt https://starwhale-examples.s3.us-west-1.amazonaws.com/model/mnist.pth) || curl -o models/mnist_cnn.pt https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/mnist.pth

.POHNY: download-data
download-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/t10k-images-idx3-ubyte ] || ([ -z $(CN) ] && curl -o data/t10k-images-idx3-ubyte https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/mnist/t10k-images-idx3-ubyte) || curl -o data/t10k-images-idx3-ubyte https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/mnist/t10k-images-idx3-ubyte
	[ -f data/t10k-labels-idx1-ubyte ] || ([ -z $(CN) ] && curl -o data/t10k-labels-idx1-ubyte https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/mnist/t10k-labels-idx1-ubyte) || curl -o data/t10k-labels-idx1-ubyte https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/mnist/t10k-labels-idx1-ubyte

.POHNY: prepare
prepare: download-model download-data
	python3 -m pip install -r requirements.txt
