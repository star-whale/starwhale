dev-all:
	yarn
	yarn build
	yarn run serve

install-req:
	yarn

build-docs:
	yarn build

lint-docs:
	yarn lint-docs

versioning-docs:
	yarn docusaurus docs:version ${RELEASE_VERSION}
	cd i18n/zh/docusaurus-plugin-content-docs/version-${RELEASE_VERSION}; rm img; ln -s ../../../../versioned_docs/version-${RELEASE_VERSION}/img img
