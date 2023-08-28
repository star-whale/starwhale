PY_CHANGED_FILES = $(shell git diff --name-only --relative -- '*.py')
DISTRIBUTOR_OS = $(shell lsb_release -si)

.PHONY: *

check:
	python3 setup.py check

clean:
	rm -rf dist/*

build-ui:
	[ "${SKIP_UI_BUILD}" = "1" ] || make -C ../console install-dev-tools
	[ "${SKIP_UI_BUILD}" = "1" ] || make -C ../console build-all

build-wheel: check clean build-ui
	python3 setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	twine upload dist/*

install-sw:
	python3 -m pip install -e '.[all]'

install-dev-req:
	python3 -m pip install -r requirements-dev.txt
	((grep -E "ubuntu|debian" /etc/os-release > /dev/null) && sudo apt-get install libsndfile1 -y) || echo "skip install libsndfile1"

black-format:
	black --config pyproject.toml $(PY_CHANGED_FILES)

isort-format:
	isort $(PY_CHANGED_FILES)

format:
	black --config pyproject.toml . ../example ../scripts/client_test && isort . ../example ../scripts/client_test

ci-format-checker:
	echo "run black"
	black --check --config pyproject.toml . ../example ../scripts/client_test

ci-lint:
	echo "run flake8"
	flake8 . ../example ../scripts/client_test

ci-mypy:
	echo "run mypy"
	mypy . ../example/mnist ../scripts/client_test --show-traceback

ci-isort:
	echo "run isort"
	isort --check . ../example ../scripts/client_test

ut:
	echo "ut"
	python -m pytest tests -vvrfEsxl --cov-config=.coveragerc --cov=starwhale --cov-report=xml:coverage.xml --cov-report=term-missing

fast-ut:
	echo "fast ut"
	python -m pytest tests -vvrfEsxl --cov-config=.coveragerc --cov=starwhale --cov-report=xml:coverage.xml --cov-report=term-missing -n 4 --dist=loadscope

all-check: ci-format-checker ci-lint ci-mypy ci-isort fast-ut
