GIT_REVISION = $(shell git rev-parse --short HEAD)
DATE = $(shell date "+%Y%m%d")
LATEST_R_VERSION = 0.1.1

DOCKER_HUB_REPO := starwhaleai
GHCR_IO_REPO := ghcr.io/star-whale

# Please update base/nodejs/taskset/base_server image version by manual, DOT NOT USE RELEASE TAG.
# These images versions are slow to release.
FIXED_VERSION_BASE_IMAGE := 0.2.0
FIXED_VERSION_NODEJS_IMAGE := 0.1.1
FIXED_VERSION_TASKSET_IMAGE := 0.1.1
FIXED_VERSION_BASE_SERVER_IMAGE := 0.1.1

DH_BASE_IMAGE := ${DOCKER_HUB_REPO}/base:${FIXED_VERSION_BASE_IMAGE}
DH_BASE_SERVER_IMAGE := ${DOCKER_HUB_REPO}/base_server:${FIXED_VERSION_BASE_SERVER_IMAGE}
DH_NODEJS_IMAGE := ${DOCKER_HUB_REPO}/nodejs:${FIXED_VERSION_NODEJS_IMAGE}

DOCKER_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := $(dir $(abspath $(DOCKER_DIR)))

ifndef RELEASE_VERSION
	RELEASE_VERSION = ${LATEST_R_VERSION}-nightly-${DATE}-${GIT_REVISION}
endif

FMT_VERSION_CODE := "import pkg_resources; _v=pkg_resources.parse_version('${RELEASE_VERSION}'); print(_v.public)"
PYPI_FMT_RELEASE_VERSION := $(shell python -c ${ONE_LINE_CODE})

ifndef PYPI_RELEASE_VERSION
	PYPI_RELEASE_VERSION = 0.1.0.dev15
endif

define build-image
	docker pull ${GHCR_IO_REPO}/$(1):latest || true
	docker build --build-arg BASE_IMAGE=$(DH_BASE_IMAGE) --build-arg BASE_SERVER_IMAGE=$(DH_BASE_SERVER_IMAGE) --build-arg SW_VERSION=$(PYPI_RELEASE_VERSION) -f Dockerfile.$(1) -t ${DOCKER_HUB_REPO}/$(1):$(2) .
	docker tag ${DOCKER_HUB_REPO}/$(1):$(2) ${DOCKER_HUB_REPO}/$(1):latest
	docker tag ${DOCKER_HUB_REPO}/$(1):$(2) ${GHCR_IO_REPO}/$(1):$(2)
	docker tag ${DOCKER_HUB_REPO}/$(1):$(2) ${GHCR_IO_REPO}/$(1):latest
	$(if $(3), $(docker tag ${DOCKER_HUB_REPO}/$(1):$(2) ${DOCKER_HUB_REPO}/$(1):$(3)),)
	$(if $(3), $(docker tag ${DOCKER_HUB_REPO}/$(1):$(2) ${GHCR_IO_REPO}/$(1):$(3)),)
endef

define push-image
	docker push ${DOCKER_HUB_REPO}/$(1):$(2)
	docker push ${DOCKER_HUB_REPO}/$(1):latest
	docker push ${GHCR_IO_REPO}/$(1):latest
	docker push ${GHCR_IO_REPO}/$(1):$(2)
	$(if $(3), $(docker push ${DOCKER_HUB_REPO}/$(1):$(3)),)
	$(if $(3), $(docker push ${GHCR_IO_REPO}/$(1):$(3)),)
endef

build-base:
	$(call build-image,base,${FIXED_VERSION_BASE_IMAGE})

release-base:
	$(call push-image,base,${FIXED_VERSION_BASE_IMAGE})

build-base-server:
	$(call build-image,base_server,${FIXED_VERSION_BASE_SERVER_IMAGE})

release-base-server:
	$(call push-image,base_server,${FIXED_VERSION_BASE_SERVER_IMAGE})

build-starwhale:
	$(call build-image,starwhale,${RELEASE_VERSION},${PYPI_FMT_RELEASE_VERSION})

release-starwhale:
	$(call push-image,starwhale,${RELEASE_VERSION})

build-taskset:
	$(call build-image,taskset,${FIXED_VERSION_TASKSET_IMAGE})

release-taskset:
	$(call push-image,taskset,${FIXED_VERSION_TASKSET_IMAGE})

build-server:
	$(call build-image,server,${RELEASE_VERSION})

build-server-all: build-console build-jar
	$(call build-image,server,${RELEASE_VERSION})

release-server:
	$(call push-image,server,${RELEASE_VERSION})

build-nodejs:
	$(call build-image,nodejs,${FIXED_VERSION_NODEJS_IMAGE})

release-nodejs:
	$(call push-image,nodejs,${FIXED_VERSION_NODEJS_IMAGE})

build-console:
	docker run --rm -it -v $(ROOT_DIR)console:/app -w /app ${DH_NODEJS_IMAGE} bash -c "yarn install && yarn build"

build-jar:
	docker volume create --name maven-repo; docker run --rm -it -v maven-repo:/root/.m2 -v $(ROOT_DIR):/app -w /app maven:3.8.5-openjdk-11 mvn clean package -f server/pom.xml -DskipTests;