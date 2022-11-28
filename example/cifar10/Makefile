.POHNY: train
train:
	mkdir -p models data
	python3 cifar/train.py


.POHNY: prepare-data
prepare-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/cifar-10-python.tar.gz ] || ([ -z $(CN) ] && curl -o data/cifar-10-python.tar.gz https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/cifar/cifar-10-python.tar.gz) || curl -o data/cifar-10-python.tar.gz https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/cifar/cifar-10-python.tar.gz
	[ -d data/cifar-10-batches-py ] || tar -xzf data/cifar-10-python.tar.gz -C data
	[ -d models ] ||  mkdir -p models
	[ -f models/cifar_net.pth ] || ([ -z $(CN) ] && curl -o models/cifar_net.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/cifar_net.pth) || curl -o models/cifar_net.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/cifar_net.pth
	python3 -m pip install -r requirments.txt
