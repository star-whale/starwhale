.POHNY: train
train:
	mkdir -p models data
	python3 sc/train.py

.POHNY: prepare-data
prepare-data:
	[ -d data/SpeechCommands/speech_commands_v0.02 ] ||  mkdir -p data/SpeechCommands/speech_commands_v0.02
	[ -f data/speech_commands_v0.02.tar.gz ] || ([ -z $(CN) ] && curl -o data/speech_commands_v0.02.tar.gz https://starwhale-examples.s3.us-west-1.amazonaws.com/dataset/SpeechCommands/speech_commands_v0.02.tar.gz) || curl -o data/speech_commands_v0.02.tar.gz https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/SpeechCommands/speech_commands_v0.02.tar.gz
	[ -f data/SpeechCommands/speech_commands_v0.02/validation_list.txt ] || tar -xzf data/speech_commands_v0.02.tar.gz -C data/SpeechCommands/speech_commands_v0.02
	[ -d models ] ||  mkdir -p models
	[ -f models/m5.pth ] || ([ -z $(CN) ] && curl -o models/m5.pth https://starwhale-examples.s3.us-west-1.amazonaws.com/model/speech_cmd_m5.pth) || curl -o models/m5.pth https://starwhale-examples.oss-cn-beijing.aliyuncs.com/model/speech_cmd_m5.pth


