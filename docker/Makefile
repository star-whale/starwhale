BASE_IMAGE :=starwhaleai/base:0.1.0-nightly-2022042701
SW_IMAGE := starwhaleai/starwhale:0.1.0-nightly-2022042701
SW_TASKSET_IMAGE := starwhaleai/taskset:0.1.0-nightly-2022041601
SW_CONTROLLER_IMAGE := starwhaleai/controller:0.1.0-nightly-2022042701
SW_AGENT_IMAGE := starwhaleai/agent:0.1.0-nightly-2022042704
SW_VERSION := 0.1.0.dev12
DOCKER_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := $(dir $(abspath $(DOCKER_DIR)))

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

build-jar:
	docker volume create --name maven-repo; docker run --rm -it -v maven-repo:/root/.m2 -v $(ROOT_DIR):/opt/starwhale maven:3.8.5-openjdk-11 mvn clean package -f /opt/starwhale/server/pom.xml -DskipTests;

build-controller:
	docker build -f Dockerfile.controller -t $(SW_CONTROLLER_IMAGE) .

build-agent:
	docker build -f Dockerfile.agent -t $(SW_AGENT_IMAGE) .