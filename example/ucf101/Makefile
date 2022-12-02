.POHNY: train
train:
	mkdir -p models
	python3 ucf101/train.py

.POHNY: download-data
download-data:
	rm -rf data
	mkdir -p data
	wget http://www.crcv.ucf.edu/data/UCF101/UCF101.rar --no-check-certificate -P data
	unrar x data/UCF101.rar data
	rm -rf data/UCF101.rar
	rm -f data/all_list.txt
	bash generate_data.sh
	shuf data/all_list.txt -n 9000 -o data/train_list.txt
	shuf data/all_list.txt -n 1000 -o data/validation_list.txt
	shuf data/all_list.txt -n 200 -o data/test_list.txt

.POHNY: prepare-data
prepare-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/data.zip ] || ([ -z $(CN) ] && curl -o data/data.zip https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/ucf101/data.zip) || curl -o data/data.zip https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/ucf101/data.zip
	[ -f data/test_list.txt ] || unzip data/data.zip -d .
	[ -d data/UCF-101 ] || mv data/UCF-101-WEBM data/UCF-101
	[ -d models ] ||  mkdir -p models
	[ -f models/PyTorch-MFNet_ep-0000.pth ] || ([ -z $(CN) ] && curl -o models/PyTorch-MFNet_ep-0000.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/ucf101.pth) || curl -o models/PyTorch-MFNet_ep-0000.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/ucf101.pth
	python3 -m pip install -r requirements.txt
