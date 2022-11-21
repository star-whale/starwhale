.POHNY: train
train:
	mkdir -p models
	python3 main.py

.POHNY: download-data
download-data:
	rm -rf data
	mkdir -p data
	curl -o data/fra-eng.zip https://www.manythings.org/anki/fra-eng.zip
	unzip data/fra-eng.zip -d data
	rm -rf data/fra-eng.zip
	shuf -n 1000 data/fra.txt > data/fra-test.txt

.POHNY: prepare-e2e-data
prepare-e2e-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/fra-eng.zip ] || curl -o data/fra-eng.zip http://10.131.0.1:9000/users/dataset/nmt/fra-eng.zip
	[ -f data/fra.txt ] || unzip data/fra-eng.zip -d data
	[ -f data/fra-test.txt ] || shuf -n 1000 data/fra.txt > data/fra-test.txt
	[ -d models ] ||  mkdir -p models
	[ -f models/decoder.pth ] || curl -o models/decoder.pth http://10.131.0.1:9000/users/model/nmt/decoder.pth
	[ -f models/encoder.pth ] || curl -o models/encoder.pth http://10.131.0.1:9000/users/model/nmt/encoder.pth
	[ -f models/vocab_eng-fra.bin ] || curl -o models/vocab_eng-fra.bin http://10.131.0.1:9000/users/model/nmt/vocab_eng-fra.bin

