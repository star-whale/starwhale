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
