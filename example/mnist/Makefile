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

ppl-fuse-test:
	SW_TASK_SWDS_CONFIG=./test/swds_fuse.json SW_TASK_STATUS_DIR=./test/task_volume/status \
	SW_TASK_LOG_DIR=./test/task_volume/log SW_TASK_RESULT_DIR=./test/task_volume/result \
	python mnist/ppl.py

ppl-s3-test:
	SW_TASK_SWDS_CONFIG=./test/swds_s3.json SW_TASK_STATUS_DIR=./test/task_volume/status \
	SW_TASK_LOG_DIR=./test/task_volume/log SW_TASK_RESULT_DIR=./test/task_volume/result \
	python mnist/ppl.py