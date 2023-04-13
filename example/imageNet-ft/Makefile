.POHNY: download-model
download-model:
	[ -d models ] ||  mkdir -p models
	[ -f models/resnet18.pth ] || curl -o models/resnet18.pth https://s3.amazonaws.com/pytorch/models/resnet18-f37072fd.pth

.POHNY: download-data
download-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/hotdog.zip ] || curl -o data/hotdog.zip https://d2l-data.s3-accelerate.amazonaws.com/hotdog.zip
	[ -d data/train && -d data/test ] ||  (unzip data/hotdog.zip -d . && mv hotdog/* data/ && rm -r hotdog)

.POHNY: prepare
prepare: download-model download-model
	python3 -m pip install -r requirements.txt
