.POHNY: build-base
build-base:
	docker build . -f Dockerfile.cuda_base -t jetson:cuda

.POHNY: build-runtime-image
build-runtime-image:
	docker build . -f Dockerfile.runtime --build-arg=BASE_IMAGE=jetson:cuda -t jetson:runtime

.POHNY: build-runtime
build-runtime:
	swcli runtime build -f demo-runtime-mnist.yaml .
