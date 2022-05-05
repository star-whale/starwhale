GIT_REVISION = $(shell git rev-parse --short HEAD)
DATE = $(shell date "+%Y%m%d%H%M%S")
BASE_VERSION = 0.1.0
VERSION_FORMAT = ${BASE_VERSION}-nightly-${DATE}-${GIT_REVISION}
SW_VERSION := 0.1.0.dev15

BASE_IMAGE :=starwhaleai/base:${VERSION_FORMAT}
NODEJS_IMAGE :=starwhaleai/nodejs:${VERSION_FORMAT}
SW_IMAGE := starwhaleai/starwhale:${VERSION_FORMAT}
SW_TASKSET_IMAGE := starwhaleai/taskset:${VERSION_FORMAT}
SW_CONTROLLER_IMAGE := starwhaleai/controller:${VERSION_FORMAT}
SW_AGENT_IMAGE := starwhaleai/agent:${VERSION_FORMAT}

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

# stable:starwhaleai/nodejs:0.1.0-nightly-20220505190936
build-nodejs:
	docker build -t ${NODEJS_IMAGE} -f Dockerfile.nodejs .

build-ui:
	docker run --rm -it -v $(ROOT_DIR)console:/app -w /app starwhaleai/nodejs:0.1.0-nightly-20220505190936 yarn install;
	docker run --rm -it -v $(ROOT_DIR)console:/app -w /app starwhaleai/nodejs:0.1.0-nightly-20220505190936 yarn build;

build-jar-with-ui: build-ui build-jar

build-jar:
	docker volume create --name maven-repo; docker run --rm -it -v maven-repo:/root/.m2 -v $(ROOT_DIR)server:/app -w /app maven:3.8.5-openjdk-11 mvn clean package -DskipTests;

build-controller: build-jar-with-ui
	docker build -f Dockerfile.controller -t $(SW_CONTROLLER_IMAGE) .

build-agent: build-jar
	docker build -f Dockerfile.agent -t $(SW_AGENT_IMAGE) .