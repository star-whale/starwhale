train:
	python mnist/train.py


.POHNY: prepare-e2e-data
prepare-e2e-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/t10k-images-idx3-ubyte ] || curl -o data/t10k-images-idx3-ubyte http://10.131.0.1:9000/users/dataset/mnist/t10k-images-idx3-ubyte
	[ -f data/t10k-labels-idx1-ubyte ] || curl -o data/t10k-labels-idx1-ubyte http://10.131.0.1:9000/users/dataset/mnist/t10k-labels-idx1-ubyte
	[ -d models ] ||  mkdir -p models
	[ -f models/mnist_cnn.pt ] || curl -o models/mnist_cnn.pt http://10.131.0.1:9000/users/model/mnist.pth
