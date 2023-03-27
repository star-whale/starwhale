.PHONY: *

install-dev-tools:
	npm install -g yarn pnpm

install-dependencies:
	yarn

build-ui:
	yarn build

ci-lint:
	yarn lint
	yarn typecheck

rebuild-gradio:
	rm -rf gradio && git clone --depth 1 --branch=feat/sw-gradio-widget https://github.com/star-whale/gradio gradio	&& \
	export NODE_OPTIONS="--max_old_space_size=4096" && \
	export NODE_OPTIONS="--max-old-space-size=4096" && \
	cd gradio/ui && \
		pnpm i && \
		pnpm build && \
		cd ../.. && \
		rm -rf src/assets/GradioWidget && mkdir -p src/assets/GradioWidget && \
		mv gradio/ui/packages/starwhale/build src/assets/GradioWidget/es
	rm -rf gradio

build-all: install-dependencies rebuild-gradio build-ui
