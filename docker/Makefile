GIT_REVISION = $(shell git rev-parse --short HEAD)
DATE = $(shell date "+%Y%m%d%H%M%S")
BASE_VERSION = 0.1.0
VERSION_FORMAT = ${BASE_VERSION}-nightly-${DATE}-${GIT_REVISION}
SW_VERSION := 0.1.0.dev15

BASE_IMAGE :=starwhaleai/base:${VERSION_FORMAT}
NODEJS_IMAGE :=starwhaleai/nodejs:${VERSION_FORMAT}
SW_IMAGE := starwhaleai/starwhale:${VERSION_FORMAT}
SW_LATEST_IMAGE := starwhaleai/starwhale:latest
SW_TASKSET_IMAGE := starwhaleai/taskset:${VERSION_FORMAT}
SW_SERVER_IMAGE := starwhaleai/server:${VERSION_FORMAT}

DOCKER_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := $(dir $(abspath $(DOCKER_DIR)))

build-base:
	docker build -f Dockerfile.base -t $(BASE_IMAGE) .

build-starwhale:
	docker build --build-arg BASE_IMAGE=$(BASE_IMAGE) --build-arg SW_VERSION=$(SW_VERSION) -f Dockerfile.sw -t $(SW_IMAGE) .
	docker tag $(SW_IMAGE) $(SW_LATEST_IMAGE)

release:
	docker push $(BASE_IMAGE)
	docker push $(SW_IMAGE)
	docker push $(SW_LATEST_IMAGE)
	docker push $(SW_TASKSET_IMAGE)
	docker push starwhaleai/server:${VERSION_FORMAT}

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
	docker volume create --name maven-repo; docker run --rm -it -v maven-repo:/root/.m2 -v $(ROOT_DIR):/app -w /app maven:3.8.5-openjdk-11 mvn clean package -f server/pom.xml -DskipTests;

build-server: build-jar-with-ui
	docker build -f Dockerfile.server -t $(SW_SERVER_IMAGE) .

# used when release
build-server-when-ui-and-jar-built:
	docker build -f Dockerfile.server -t $(SW_SERVER_IMAGE) .