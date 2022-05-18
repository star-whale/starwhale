ifndef RELEASE_VERSION
	RELEASE_VERSION := "0.1.0-dummy.1"
endif

ifndef CLONE_DIR
	CLONE_DIR := $(shell mktemp -d)
endif


lint:
	ct lint --all

package:
	helm package . --version ${RELEASE_VERSION} --app-version ${RELEASE_VERSION}

index:
	helm repo index .

cleanup:
	rm -rf starwhale-*.tgz index.yaml

upload:
	if [ ${GITHUB_ACTIONS} ]; then git config --global user.name "github-actions[bot]"; fi
	if [ ${GITHUB_ACTIONS} ]; then git config --global user.email "github-actions[bot]@users.noreply.github.com"; fi
	if [ ${GITHUB_ACTIONS} ]; then git config --global credential.helper store; fi
	git clone --single-branch --branch gh-pages "https://x-access-token:${API_TOKEN_GITHUB}@github.com/star-whale/charts.git" "${CLONE_DIR}"
	cp -R starwhale-${RELEASE_VERSION}.tgz ${CLONE_DIR}
	cd ${CLONE_DIR}; helm repo index .
	cd ${CLONE_DIR}; git add . && git commit --message "release charts ${RELEASE_VERSION}" && git push -f origin HEAD:gh-pages || exit 1
	rm -rf ${CLONE_DIR} || true

dry-run:
	helm install --debug --dry-run starwhale ./

template:
	helm template starwhale ./ -n starwhale --debug