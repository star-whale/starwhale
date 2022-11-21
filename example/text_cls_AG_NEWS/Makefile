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

.POHNY: prepare-e2e-data
prepare-e2e-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/test.csv ] || curl -o data/test.csv http://10.131.0.1:9000/users/dataset/ag_news/data/test.csv
	[ -d models ] ||  mkdir -p models
	[ -f models/model.i ] || curl -o models/model.i http://10.131.0.1:9000/users/model/ag_news/models/model.i
	[ -f models/vocab.i ] || curl -o models/vocab.i http://10.131.0.1:9000/users/model/ag_news/models/vocab.i

