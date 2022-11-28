.POHNY: train
train:
	mkdir -p models
	python tcan/train.py --device cpu --save-model-path  models/model.i --dictionary models/vocab.i --data-dir data --num-epochs 5

.POHNY: download-data
download-data:
	rm -rf data
	mkdir -p data
	curl -o data/train.csv https://raw.githubusercontent.com/mhjabreel/CharCnn_Keras/master/data/ag_news_csv/train.csv
	curl -o data/test.csv https://raw.githubusercontent.com/mhjabreel/CharCnn_Keras/master/data/ag_news_csv/test.csv

.POHNY: prepare-data
prepare-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/test.csv ] || ([ $(CN) = "1" ] && curl -o data/test.csv https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/ag_news/data/test.csv) || curl -o data/test.csv https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/ag_news/data/test.csv
	[ -d models ] ||  mkdir -p models
	[ -f models/model.i ] || ([ $(CN) = "1" ] && curl -o models/model.i https://starwhale-examples.s3.us-west-1.amazonaws.com/model/ag_news/models/model.i) || curl -o models/model.i https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/ag_news/models/model.i
	[ -f models/vocab.i ] || ([ $(CN) = "1" ] && curl -o models/vocab.i https://starwhale-examples.s3.us-west-1.amazonaws.com/model/ag_news/models/vocab.i) || curl -o models/vocab.i https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/ag_news/models/vocab.i

