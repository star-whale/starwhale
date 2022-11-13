.POHNY: train
train:
	mkdir -p models
	python3 ucf101/train.py

.POHNY: download-data
download-data:
	rm -rf data
	mkdir -p data
	wget http://www.crcv.ucf.edu/data/UCF101/UCF101.rar --no-check-certificate -P data
	unrar x data/UCF101.rar data
	rm -rf data/UCF101.rar
	rm -f data/all_list.txt
	bash generate_data.sh
	shuf data/all_list.txt -n 9000 -o data/train_list.txt
	shuf data/all_list.txt -n 1000 -o data/validation_list.txt
	shuf data/all_list.txt -n 200 -o data/test_list.txt
