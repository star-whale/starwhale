.POHNY: train
train:
	mkdir -p models data
	python3 sc/train.py

.POHNY: prepare-e2e-data
prepare-e2e-data:
	[ -d data ] ||  mkdir -p data
	[ -f data/speech_commands_v0.02.tar.gz ] || curl -o data/speech_commands_v0.02.tar.gz http://10.131.0.1:9000/users/dataset/SpeechCommands/speech_commands_v0.02.tar.gz
	[ -f data/validation_list.txt ] || tar -xzf data/speech_commands_v0.02.tar.gz -C data
	[ -d models ] ||  mkdir -p models
	[ -f models/m5.pth ] || curl -o models/m5.pth http://10.131.0.1:9000/users/model/speech_cmd_m5.pth


