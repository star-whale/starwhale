install-yarn:
	npm install -g yarn
install-dependencies:
	yarn
build-ui:
	yarn build
ci-lint:
	yarn lint
	yarn typecheck

rebuild-gradio:
	rm -rf gradio && git clone --depth 1 --branch=feat/sw-gradio-widget https://github.com/star-whale/gradio gradio	
	pushd gradio/ui && \
		pnpm i && \
		pnpm build && \
		rm -rf src/assets/GradioWidget && mkdir -p src/assets/GradioWidget && \
		mv packages/starwhale/build src/assets/GradioWidget/es && popd
	rm -rf gradio

