venv-create:
	python3 -m venv venv

venv-active:
	source venv/bin/activate

venv-dump:
	xx

conda-export:
	conda env export -n mnist > mnist-env-lock.yml


conda-clone:
	conda create --name pytorch2 --clone pytorch


conda-create:
	conda env create --file mnist-env-lock

train:
	python mnist/train.py

test:
	python mnist/test.py

inference:
	python mnist/ppl.py