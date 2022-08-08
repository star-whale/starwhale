GIT_REVISION = $(shell git rev-parse --short HEAD)
DATE = $(shell date "+%Y%m%d")
LATEST_R_VERSION = $(patsubst v%,%,$(shell git describe --tags --abbrev=0))

DOCKER_HUB_REPO := starwhaleai
GHCR_IO_REPO := ghcr.io/star-whale

# Please update base/nodejs/base_server image version by manual, DOT NOT USE RELEASE TAG.
# These images versions are slow to release.
FIXED_VERSION_BASE_IMAGE := 0.2.1
FIXED_VERSION_NODEJS_IMAGE := 0.1.1
FIXED_VERSION_BASE_SERVER_IMAGE := 0.1.1

GHCR_BASE_IMAGE := ${GHCR_IO_REPO}/base:${FIXED_VERSION_BASE_IMAGE}
DH_BASE_SERVER_IMAGE := ${DOCKER_HUB_REPO}/base_server:${FIXED_VERSION_BASE_SERVER_IMAGE}
DH_NODEJS_IMAGE := ${DOCKER_HUB_REPO}/nodejs:${FIXED_VERSION_NODEJS_IMAGE}

DOCKER_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := $(dir $(abspath $(DOCKER_DIR)))

ifndef RELEASE_VERSION
	RELEASE_VERSION = ${LATEST_R_VERSION}-nightly-${DATE}-${GIT_REVISION}
endif

FMT_VERSION_CODE := "import pkg_resources; _v=pkg_resources.parse_version('${RELEASE_VERSION}'); print(_v.public)"
PYPI_FMT_RELEASE_VERSION := $(shell python3 -c ${FMT_VERSION_CODE})

ifndef PYPI_RELEASE_VERSION
	PYPI_RELEASE_VERSION = 0.1.0.dev15
endif

define build-image
	docker pull ${GHCR_IO_REPO}/$(1):latest || true
	docker build \
		--build-arg BASE_IMAGE=$(GHCR_BASE_IMAGE) \
		--build-arg BASE_SERVER_IMAGE=$(DH_BASE_SERVER_IMAGE) \
		--build-arg SW_VERSION=$(PYPI_RELEASE_VERSION) \
		-t ${DOCKER_HUB_REPO}/$(1):$(2) \
		-t ${DOCKER_HUB_REPO}/$(1):latest \
		-t ${GHCR_IO_REPO}/$(1):$(2) \
		-t ${GHCR_IO_REPO}/$(1):latest \
		-f Dockerfile.$(1) .
endef

define push-image
	docker push ${DOCKER_HUB_REPO}/$(1):$(2)
	docker push ${DOCKER_HUB_REPO}/$(1):latest
	docker push ${GHCR_IO_REPO}/$(1):latest
	docker push ${GHCR_IO_REPO}/$(1):$(2)
endef

define build-starwhale
	docker buildx build --platform linux/arm64,linux/amd64 \
		--build-arg BASE_IMAGE=$(1) \
		--build-arg SW_VERSION=$(PYPI_RELEASE_VERSION) \
		--build-arg HTTP_PROXY=$(HTTP_PROXY) --build-arg HTTPS_PROXY=$(HTTPS_PROXY) \
		--build-arg http_proxy=$(HTTP_PROXY) --build-arg https_proxy=$(HTTPS_PROXY) \
		--network=host \
		-t $(DOCKER_HUB_REPO)/starwhale:$(RELEASE_VERSION)$(2) \
		-t $(DOCKER_HUB_REPO)/starwhale:$(PYPI_FMT_RELEASE_VERSION)$(2) \
		-t $(DOCKER_HUB_REPO)/starwhale:latest$(2) \
		-t $(GHCR_IO_REPO)/starwhale:$(RELEASE_VERSION)$(2) \
		-t $(GHCR_IO_REPO)/starwhale:$(PYPI_FMT_RELEASE_VERSION)$(2) \
		-t $(GHCR_IO_REPO)/starwhale:latest$(2) \
		--push -f Dockerfile.starwhale .
endef

define build-starwhale-cuda
	$(call build-starwhale,${GHCR_IO_REPO}/cuda:$(1)-base${FIXED_VERSION_BASE_IMAGE},-cuda$(1))
endef


prepare-buildx:
	docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
	docker buildx create --name starwhale-multiarch2 --driver docker-container \
		--driver-opt env.all_proxy=$(HTTP_PROXY) --driver-opt env.http_proxy=$(HTTP_PROXY) --driver-opt env.https_proxy=$(HTTPS_PROXY) \
		--driver-opt env.ALL_PROXY=$(HTTP_PROXY) --driver-opt env.HTTP_PROXY=$(HTTP_PROXY) --driver-opt env.HTTPS_PROXY=$(HTTPS_PROXY) \
		--use --bootstrap --driver-opt network=host

build-release-base:
	docker buildx build --platform linux/arm64,linux/amd64 \
		--build-arg HTTP_PROXY=$(HTTP_PROXY) --build-arg HTTPS_PROXY=$(HTTPS_PROXY) \
		--build-arg http_proxy=$(HTTP_PROXY) --build-arg https_proxy=$(HTTPS_PROXY) \
		--network=host \
		-t $(DOCKER_HUB_REPO)/base:$(FIXED_VERSION_BASE_IMAGE) \
		-t $(DOCKER_HUB_REPO)/base:latest \
		-t $(GHCR_IO_REPO)/base:$(FIXED_VERSION_BASE_IMAGE) \
		-t $(GHCR_IO_REPO)/base:latest \
		--push -f Dockerfile.base .

build-release-starwhale:
	$(call build-starwhale,${GHCR_IO_REPO}/base:${FIXED_VERSION_BASE_IMAGE},)

build-release-starwhale-cuda:
	$(call build-starwhale-cuda,$(version))

build-cuda:
	cd cuda; python3 render.py --all --push --base-image $(GHCR_BASE_IMAGE)
	bash cuda/.dfs/docker-build.sh

build-base-server:
	$(call build-image,base_server,${FIXED_VERSION_BASE_SERVER_IMAGE})

release-base-server:
	$(call push-image,base_server,${FIXED_VERSION_BASE_SERVER_IMAGE})

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
