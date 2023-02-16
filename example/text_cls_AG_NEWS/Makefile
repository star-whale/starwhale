.POHNY: train
train:
	mkdir -p models
	python tcan/train.py --device cpu --save-model-path  models/model.i --dictionary models/vocab.i --data-dir data --num-epochs 5

.POHNY: download-data
download-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/test.csv ] || ([ -z $(CN) ] && curl -o data/test.csv https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/ag_news/data/test.csv) || curl -o data/test.csv https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/ag_news/data/test.csv

.POHNY: download-model
download-model:
	[ -d models ] ||  mkdir -p models
	[ -f models/model.i ] || ([ -z $(CN) ] && curl -o models/model.i https://starwhale-examples.s3.us-west-1.amazonaws.com/model/ag_news/models/model.i) || curl -o models/model.i https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/ag_news/models/model.i
	[ -f models/vocab.i ] || ([ -z $(CN) ] && curl -o models/vocab.i https://starwhale-examples.s3.us-west-1.amazonaws.com/model/ag_news/models/vocab.i) || curl -o models/vocab.i https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/ag_news/models/vocab.i

.POHNY: prepare
prepare: download-model download-data
	python3 -m pip install -r requirements.txt

