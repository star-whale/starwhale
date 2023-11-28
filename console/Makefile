.PHONY: *
PROXY ?= http://127.0.0.1:5173

ut:
	yarn test:coverage

install-dev-tools:
	npm install -g yarn pnpm@latest

install-dependencies:
	yarn

OPEN_API_URL ?= "https://cloud.starwhale.cn"
gen-api:
	./node_modules/.bin/swagger-typescript-api \
		-p $(OPEN_API_URL)/v3/api-docs/Api \
		--no-client \
		-r \
		--union-enums \
		--type-prefix 'I' \
		--templates src/api/template \
		--unwrap-response-data \
		--axios \
		--single-http-client \
		--responses \
		--extract-response-body \
		--modular \
		--output src/api/server

e2e-local:
	cd playwright && yarn && npx playwright install && \
		yarn test

e2e-docker:
	docker run -it --rm --ipc=host -w /app -e PROXY=${PROXY} -v ./playwright:/app mcr.microsoft.com/playwright:v1.33.0-jammy /bin/bash -c "yarn && yarn test" || exit 1

prepare:
	mkdir -p extensions && \
		([ ! -f "extensions/index.tsx" ] && echo 'export default {}' > extensions/index.tsx) || echo 'extensions exists'

build-ui: prepare
	yarn build

ci-lint: prepare
	yarn lint
	yarn typecheck

build-all: install-dependencies build-ui
