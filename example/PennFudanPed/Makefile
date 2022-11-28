.POHNY: train
train:
	mkdir -p models
	python3 pfp/train.py

.POHNY: download-data
download-data:
	rm -rf data
	mkdir -p data
	curl -o data/pfp.zip https://www.cis.upenn.edu/~jshi/ped_html/PennFudanPed.zip
	unzip data/pfp.zip -d data
	rm -rf data/pfp.zip

.POHNY: prepare-data
prepare-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/PennFudanPed.zip ] || ([ -z $(CN) ] && curl -o data/PennFudanPed.zip https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/PennFudanPed/PennFudanPed.zip ) || curl -o data/PennFudanPed.zip https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/PennFudanPed/PennFudanPed.zip
	[ -d data/PennFudanPed ] || unzip data/PennFudanPed.zip -d data
	[ -d models ] ||  mkdir -p models
	[ -f models/mcrnn.pth ] || ([ -z $(CN) ] && curl -o models/mcrnn.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/mcrnn_fudan.pth ) || curl -o models/mcrnn.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/mcrnn_fudan.pth
	python3 -m pip install -r requirements.txt
