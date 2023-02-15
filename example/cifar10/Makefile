.POHNY: train
train:
	mkdir -p models data
	python3 cifar/train.py

.POHNY: download-model
download-model:
	[ -d models ] ||  mkdir -p models
	[ -f models/cifar_net.pth ] || ([ -z $(CN) ] && curl -o models/cifar_net.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/cifar_net.pth) || curl -o models/cifar_net.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/cifar_net.pth

.POHNY: download-data
download-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/cifar-10-python.tar.gz ] || ([ -z $(CN) ] && curl -o data/cifar-10-python.tar.gz https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/cifar/cifar-10-python.tar.gz) || curl -o data/cifar-10-python.tar.gz https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/cifar/cifar-10-python.tar.gz
	[ -d data/cifar-10-batches-py ] || tar -xzf data/cifar-10-python.tar.gz -C data

.POHNY: prepare
prepare: download-model download-data
	python3 -m pip install -r requirements.txt
