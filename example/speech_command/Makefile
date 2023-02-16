.POHNY: train
train:
	mkdir -p models data
	python3 sc/train.py

.POHNY: download-model
download-model:
	[ -d models ] ||  mkdir -p models
	[ -f models/m5.pth ] || ([ -z $(CN) ] && curl -o models/m5.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/speech_cmd_m5.pth) || curl -o models/m5.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/speech_cmd_m5.pth

.POHNY: download-data
download-data:
	[ -d data/SpeechCommands/speech_commands_v0.02 ] ||  mkdir -p data/SpeechCommands/speech_commands_v0.02
	[ -f data/speech_commands_v0.02.tar.gz ] || ([ -z $(CN) ] && curl -o data/speech_commands_v0.02.tar.gz https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/SpeechCommands/speech_commands_v0.02.tar.gz) || curl -o data/speech_commands_v0.02.tar.gz https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/SpeechCommands/speech_commands_v0.02.tar.gz
	[ -f data/SpeechCommands/speech_commands_v0.02/validation_list.txt ] || tar -xzf data/speech_commands_v0.02.tar.gz -C data/SpeechCommands/speech_commands_v0.02

.POHNY: prepare
prepare: download-model download-data
	python3 -m pip install -r requirements.txt


