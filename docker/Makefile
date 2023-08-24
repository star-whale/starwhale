SHELL=/bin/sh
GIT_REVISION = $(shell git rev-parse --short HEAD)
GIT_INFO = $(shell git config --get remote.origin.url | rev | cut -c 5- | rev)/tree/$(shell git branch --show-current)
DATE = $(shell date "+%Y%m%d")
LATEST_R_VERSION = $(patsubst v%,%,$(shell git describe --tags --abbrev=0))

DOCKER_HUB_REPO := starwhaleai
GHCR_IO_REPO := ghcr.io/star-whale

# Please update base/nodejs/base_server image version by manual, DOT NOT USE RELEASE TAG.
# These images versions are slow to release.
# When you want to release a new version, please update the FIXED_RELEASE_BASE_IMAGE_VERSION field in client/starwhale/consts/__init__.py.
FIXED_VERSION_BASE_IMAGE := 0.3.4
FIXED_VERSION_NODEJS_IMAGE := 0.1.4
FIXED_VERSION_BASE_SERVER_IMAGE := 0.1.2

GHCR_BASE_IMAGE := ${GHCR_IO_REPO}/base:${FIXED_VERSION_BASE_IMAGE}
DH_BASE_SERVER_IMAGE := ${DOCKER_HUB_REPO}/base_server:${FIXED_VERSION_BASE_SERVER_IMAGE}
DH_NODEJS_IMAGE := ${GHCR_IO_REPO}/nodejs:${FIXED_VERSION_NODEJS_IMAGE}

DOCKER_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := $(shell dirname $(abspath $(DOCKER_DIR)))

ifndef YARN_REGISTRY
	YARN_REGISTRY = https://registry.npmmirror.com
endif

ifndef RELEASE_VERSION
	RELEASE_VERSION = ${LATEST_R_VERSION}-nightly-${DATE}-${GIT_REVISION}
endif

FMT_VERSION_CODE := "import pkg_resources; _v=pkg_resources.parse_version('${RELEASE_VERSION}'); print(_v.public)"
PYPI_FMT_RELEASE_VERSION := $(shell python3 -c ${FMT_VERSION_CODE})

ifndef PYPI_RELEASE_VERSION
	PYPI_RELEASE_VERSION = 0.1.0.dev15
endif


# build multi-arch images
# args:
# 	base image, like starwhale:base, leave empty if no base image
#	Dockerfile for -f option
#	tag list split by space, e.g. starwhale:foo starwhale:bar
define build-image
	docker buildx build --platform linux/arm64,linux/amd64 \
		--network=host \
		--build-arg BASE_IMAGE=$(1) \
		--build-arg SW_VERSION=$(PYPI_RELEASE_VERSION) \
		--build-arg SW_SERVER_VERSION=$(RELEASE_VERSION) \
		--build-arg GIT_INFO=$(GIT_INFO) \
		$(if $(HTTP_PROXY), $(foreach p, HTTP_PROXY HTTPS_PROXY http_proxy https_proxy, --build-arg $(p)=$(HTTP_PROXY))) \
		$(foreach repo, ${DOCKER_HUB_REPO} ${GHCR_IO_REPO}, $(foreach tag, $(3), -t $(repo)/$(tag)) ) \
		-f $(2) \
		--push .
endef

prepare-buildx:
	docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
	docker buildx create --name starwhale-multiarch2 --driver docker-container \
		--driver-opt env.all_proxy=$(HTTP_PROXY) --driver-opt env.http_proxy=$(HTTP_PROXY) --driver-opt env.https_proxy=$(HTTPS_PROXY) \
		--driver-opt env.ALL_PROXY=$(HTTP_PROXY) --driver-opt env.HTTP_PROXY=$(HTTP_PROXY) --driver-opt env.HTTPS_PROXY=$(HTTPS_PROXY) \
		--use --bootstrap --driver-opt network=host

build-release-base:
	# build if not exist
	docker manifest inspect $(GHCR_IO_REPO)/base:$(FIXED_VERSION_BASE_IMAGE) || \
	$(call build-image,,Dockerfile.base,base:${FIXED_VERSION_BASE_IMAGE} base:latest)

build-cuda:
	cd cuda; python3 render.py --all --push --base-image $(GHCR_BASE_IMAGE)
	bash cuda/.dfs/docker-build.sh

build-release-base-server:
	docker manifest inspect $(GHCR_IO_REPO)/base_server:$(FIXED_VERSION_BASE_SERVER_IMAGE) || \
	$(call build-image,,Dockerfile.base_server,base_server:${FIXED_VERSION_BASE_SERVER_IMAGE} base_server:latest)

build-server:
	$(call build-image,$(GHCR_IO_REPO)/base_server:latest,Dockerfile.server,server:${RELEASE_VERSION} server:latest)

build-dataset-builder:
	$(call build-image,$(GHCR_IO_REPO)/base:latest,Dockerfile.dataset_build,dataset_builder:${RELEASE_VERSION} dataset_builder:latest)

build-runtime-dockerizing-builder:
	$(call build-image,$(GHCR_IO_REPO)/base:latest,Dockerfile.Dockerfile.runtime_dockerizing,runtime-dockerizing:${RELEASE_VERSION} runtime-dockerizing:latest)

build-server-all: build-console build-jar build-server

build-server-for-local-usage:
	docker build \
		--network=host \
		--build-arg BASE_IMAGE=$(DH_BASE_SERVER_IMAGE) \
		--build-arg SW_SERVER_VERSION=$(RELEASE_VERSION) \
		--build-arg GIT_INFO=$(GIT_INFO) \
		-t starwhaleai/server:latest \
		-f Dockerfile.server .

build-nodejs:
	docker manifest inspect $(GHCR_IO_REPO)/nodejs:${FIXED_VERSION_NODEJS_IMAGE} || \
	$(call build-image,,Dockerfile.nodejs,nodejs:${FIXED_VERSION_NODEJS_IMAGE} nodejs:latest)

YARN_VOLUME=yarn-cache
build-console: build-gradio-ui
	docker volume create --name ${YARN_VOLUME} && \
	docker run --rm -v ${YARN_VOLUME}:/app ${DH_NODEJS_IMAGE} /bin/sh -c "cp -r /root/.npmrc /app/ && chown $(shell id -u):$(shell id -g) -R /app" && \
	docker run --rm \
		-u $(shell id -u):$(shell id -g) \
		-v ${YARN_VOLUME}:/var/yarn-cache \
		-v ${ROOT_DIR}/console:/app \
		-e HOME=/var/yarn-cache \
		--network host \
		-w /app ${DH_NODEJS_IMAGE} \
		/bin/sh -c "npm config set registry ${YARN_REGISTRY} && yarn config set registry ${YARN_REGISTRY} && yarn config set network-timeout 600000 -g && yarn && yarn build"

MVN_IMG=maven:3.8.5-openjdk-11
MVN_VOLUME=maven-repo
build-jar:
	# clean the console assets
	rm -rvf ${ROOT_DIR}/server/controller/src/main/resources/static/*
	docker volume create --name ${MVN_VOLUME} && \
	docker run --rm -v ${MVN_VOLUME}:/app ${MVN_IMG} /bin/sh -c "chown $(shell id -u):$(shell id -g) -R /app" && \
	docker run --rm -u $(shell id -u):$(shell id -g) \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		-v ${MVN_VOLUME}:/var/maven/.m2 \
		-v ${ROOT_DIR}/:/app \
		--network host \
		-w /app/server ${MVN_IMG} \
		mvn -Duser.home=/var/maven clean package -f pom.xml -DskipTests

build-gradio-ui:
	$(eval GRADIO_DIR="${ROOT_DIR}/gradio")
	$(eval UI_DEST="/starwhale/console/src/assets/GradioWidget")
	$(eval SWU=$(shell id -u))
	$(eval SWG=$(shell id -g))
	docker volume create --name ${YARN_VOLUME}
	# use the latest version of starwhale version gradio
	git config --global --add safe.directory $(realpath ${GRADIO_DIR}) || echo "gradio not exists"
	(([ -d ${GRADIO_DIR} ] && git -C ${GRADIO_DIR} clean -f -x && git -C ${GRADIO_DIR} checkout . && git -C ${GRADIO_DIR} reset --hard && git -C ${GRADIO_DIR} pull) || (rm -rf ${GRADIO_DIR} &&git clone --depth 1 --branch=feat/sw-gradio-widget https://github.com/star-whale/gradio ${GRADIO_DIR}))
	# TODO use non root user to run the building, pnpm should be supported in the building image
	docker run --rm \
		-v ${YARN_VOLUME}:/var/yarn-cache \
		-e HOME=/var/yarn-cache \
		-v ${ROOT_DIR}:/starwhale \
		-w /starwhale/gradio/ui \
		${DH_NODEJS_IMAGE} \
		/bin/sh -x -c "npm i -g pnpm && pnpm add -g pnpm@8.0.0 && pnpm i && NODE_OPTIONS=--max_old_space_size=4048 pnpm build && \
			rm -rf ${UI_DEST} && mkdir -p ${UI_DEST} && \
			mv packages/starwhale/build ${UI_DEST}/es && \
			chown ${SWU}:${SWG} -R ${UI_DEST} && \
			chown ${SWU}:${SWG} -R /starwhale/gradio"
	git -C ${GRADIO_DIR} clean -f -x

