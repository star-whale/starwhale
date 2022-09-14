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
