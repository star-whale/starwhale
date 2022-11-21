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

.POHNY: prepare-e2e-data
prepare-e2e-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/PennFudanPed.zip ] || curl -o data/PennFudanPed.zip http://10.131.0.1:9000/users/dataset/PennFudanPed/PennFudanPed.zip
	[ -d data/PennFudanPed ] || unzip data/PennFudanPed.zip -d data
	[ -d models ] ||  mkdir -p models
	[ -f models/mcrnn.pth ] || curl -o models/mcrnn.pth http://10.131.0.1:9000/users/model/mcrnn_fudan.pth
	python3 -m pip install -r requirments.txt
