BASE_IMAGE :=starwhaleai/base:0.1.0-nightly-2022041501
SW_IMAGE := starwhaleai/starwhale:0.1.0-nightly-202204201
SW_TASKSET_IMAGE := starwhaleai/taskset:0.1.0-nightly-2022041601
SW_VERSION := 0.1.0.dev10

build-base:
	docker build -f Dockerfile.base -t $(BASE_IMAGE) .

build-starwhale:
	docker build --build-arg BASE_IMAGE=$(BASE_IMAGE) --build-arg SW_VERSION=$(SW_VERSION) -f Dockerfile.sw -t $(SW_IMAGE) .

release:
	docker push $(BASE_IMAGE)
	docker push $(SW_IMAGE)
	docker push $(SW_TASKSET_IMAGE)

build-taskset:
	docker build --build-arg BASE_IMAGE=$(BASE_IMAGE) -f Dockerfile.taskset -t $(SW_TASKSET_IMAGE) .
