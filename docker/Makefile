BASE_IMAGE :=starwhaleai/base:0.1.0-nightly-2022041301
SW_IMAGE := starwhaleai/starwhale:0.1.0-nightly-2022041403
SW_VERSION := 0.1.0.dev9

build-base:
	docker build -f Dockerfile.base -t $(BASE_IMAGE) .

build-starwhale:
	docker build --build-arg BASE_IMAGE=$(BASE_IMAGE) --build-arg SW_VERSION=$(SW_VERSION) -f Dockerfile.sw -t $(SW_IMAGE) .

release:
	docker push $(BASE_IMAGE)
	docker push $(SW_IMAGE)
