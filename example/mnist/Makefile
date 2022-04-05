TEST_ROOT := test
TASK_VOLUME_ROOT := $(TEST_ROOT)/task_volume
SW_TASK_ENV := env SW_TASK_STATUS_DIR=$(TASK_VOLUME_ROOT)/status SW_TASK_LOG_DIR=$(TASK_VOLUME_ROOT)/log SW_TASK_RESULT_DIR=$(TASK_VOLUME_ROOT)/result

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

clean:
	rm -rf test/task_volume/*

inference: clean
	$(SW_TASK_ENV) swcli model ppl . --swds-config $(TEST_ROOT)/swds_s3_smoke.json

inference-s3: clean
	$(SW_TASK_ENV) swcli model ppl . --swds-config $(TEST_ROOT)/swds_s3.json

inference-fuse: clean
	$(SW_TASK_ENV) swcli model ppl . --swds-config $(TEST_ROOT)/swds_fuse.json

inference-all: inference inference-fuse inference-s3