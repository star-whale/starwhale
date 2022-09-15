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
