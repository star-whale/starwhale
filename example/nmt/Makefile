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

.POHNY: prepare-data
prepare-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/fra-eng.zip ] || ([ -z $(CN) ] && curl -o data/fra-eng.zip https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/nmt/fra-eng.zip) || curl -o data/fra-eng.zip https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/nmt/fra-eng.zip
	[ -f data/fra.txt ] || unzip data/fra-eng.zip -d data
	[ -f data/fra-test.txt ] || shuf -n 1000 data/fra.txt > data/fra-test.txt
	[ -d models ] ||  mkdir -p models
	[ -f models/decoder.pth ] || ([ -z $(CN) ] && curl -o models/decoder.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/nmt/decoder.pth) || curl -o models/decoder.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/nmt/decoder.pth
	[ -f models/encoder.pth ] || ([ -z $(CN) ] && curl -o models/encoder.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/nmt/encoder.pth) || curl -o models/encoder.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/nmt/encoder.pth
	[ -f models/vocab_eng-fra.bin ] || ([ -z $(CN) ] && curl -o models/vocab_eng-fra.bin https://starwhale-examples.s3.us-west-1.amazonaws.com/model/nmt/vocab_eng-fra.bin) || curl -o models/vocab_eng-fra.bin https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/nmt/vocab_eng-fra.bin
	python3 -m pip install -r requirments.txt

