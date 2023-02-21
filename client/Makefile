PY_CHANGED_FILES = $(shell git diff --name-only --relative -- '*.py')
DISTRIBUTOR_OS = $(shell lsb_release -si)

.POHNY: check
check:
	python3 setup.py check

.POHNY: clean
clean:
	rm -rf dist/*

build-wheel: check clean
	python3 setup.py sdist bdist_wheel
	ls -alh dist

.POHNY: upload-pypi
upload-pypi:
	twine upload dist/*

.POHNY: install-sw
install-sw:
	python3 -m pip install -e .

.POHNY: install-dev-req
install-dev-req:
	python3 -m pip install -r requirements-dev.txt
	((grep -E "ubuntu|debian" /etc/os-release > /dev/null) && sudo apt-get install libsndfile1) || echo "skip install libsndfile1"

.POHNY: black-format
black-format:
	black --config pyproject.toml $(PY_CHANGED_FILES)

.POHNY: isort-format
isort-format:
	isort $(PY_CHANGED_FILES)

.POHNY: format
format:
	black --config pyproject.toml . ../example ../scripts/client_test && isort . ../example ../scripts/client_test

.POHNY: ci-format-checker
ci-format-checker:
	echo "run black"
	black --check --config pyproject.toml . ../example ../scripts/client_test

.POHNY: ci-lint
ci-lint:
	echo "run flake8"
	flake8 . ../example ../scripts/client_test

.POHNY: ci-mypy
ci-mypy:
	echo "run mypy"
	mypy . ../example/mnist ../scripts/client_test

.POHNY: ci-isort
ci-isort:
	echo "run isort"
	isort --check . ../example ../scripts/client_test

.POHNY: ut
ut:
	echo "ut"
	python -m pytest tests -vvrfEsxl --cov-config=.coveragerc --cov=starwhale --cov-report=xml:coverage.xml --cov-report=term-missing

.POHNY: fast-ut
fast-ut:
	echo "fast ut"
	python -m pytest tests -vvrfEsxl --cov-config=.coveragerc --cov=starwhale --cov-report=xml:coverage.xml --cov-report=term-missing -n 4 --dist=loadscope

.POHNY: all-check
all-check: ci-format-checker ci-lint ci-mypy ci-isort fast-ut
