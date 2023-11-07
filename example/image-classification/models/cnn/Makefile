.POHNY: train
train:
	mkdir -p models data
	swcli runtime activate image-classification
	python3 train.py

.POHNY: download-model
download-model:
	[ -d models ] ||  mkdir -p models
	[ -f models/cifar_net.pth ] || ([ -z $(CN) ] && curl -o models/cifar_net.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/cifar_net.pth) || curl -o models/cifar_net.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/cifar_net.pth