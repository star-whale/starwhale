.POHNY: train
train:
	mkdir -p models data
	python3 cifar/train.py


.POHNY: prepare-e2e-data
prepare-e2e-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/cifar-10-python.tar.gz ] || curl -o data/cifar-10-python.tar.gz http://10.131.0.1:9000/users/dataset/cifar/cifar-10-python.tar.gz
	[ -d data/cifar-10-batches-py ] || tar -xzf data/cifar-10-python.tar.gz -C data
	[ -d models ] ||  mkdir -p models
	[ -f models/cifar_net.pth ] || curl -o models/cifar_net.pth http://10.131.0.1:9000/users/model/cifar_net.pth
	python3 -m pip install -r requirments.txt
