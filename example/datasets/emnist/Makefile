.POHNY: download
download:
	mkdir -p data
	[ -f data/emnist-gzip.zip ] || wget https://rds.westernsydney.edu.au/Institutes/MARCS/BENS/EMNIST/emnist-gzip.zip -O data/emnist-gzip.zip
	[ -d data/gzip ] || unzip data/emnist-gzip.zip -d data
	gunzip data/gzip/*.gz
