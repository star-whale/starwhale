BASE_IMAGE :=starwhaleai/base:0.1.0-nightly-2022041501
SW_IMAGE := starwhaleai/starwhale:0.1.0-nightly-2022042601
SW_TASKSET_IMAGE := starwhaleai/taskset:0.1.0-nightly-2022041601
SW_CONTROLLER_IMAGE := starwhaleai/controller:0.1.0-nightly-2022042501
SW_AGENT_IMAGE := starwhaleai/agent:0.1.0-nightly-2022042501
SW_VERSION := 0.1.0.dev12

build-base:
	docker build -f Dockerfile.base -t $(BASE_IMAGE) .

build-starwhale:
	docker build --build-arg BASE_IMAGE=$(BASE_IMAGE) --build-arg SW_VERSION=$(SW_VERSION) -f Dockerfile.sw -t $(SW_IMAGE) .

release:
	docker push $(BASE_IMAGE)
	docker push $(SW_IMAGE)
	docker push $(SW_TASKSET_IMAGE)
	docker push $(SW_CONTROLLER_IMAGE)
	docker push $(SW_AGENT_IMAGE)

build-taskset:
	docker build --build-arg BASE_IMAGE=$(BASE_IMAGE) -f Dockerfile.taskset -t $(SW_TASKSET_IMAGE) .

build-mvn:
	mvn clean package -f ../server/pom.xml -Dmaven.test.skip=true

build-controller:
	docker build -f Dockerfile.controller -t $(SW_CONTROLLER_IMAGE) .
build-agent:
	docker build --build-arg NV_DRIVER_VERSION=470 -f Dockerfile.agent -t $(SW_AGENT_IMAGE) .