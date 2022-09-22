PY_CHANGED_FILES = $(shell git diff --name-only --relative -- '*.py')

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

.POHNY: black-format
black-format:
	black --config pyproject.toml $(PY_CHANGED_FILES)

.POHNY: isort-format
isort-format:
	isort $(PY_CHANGED_FILES)

.POHNY: format
format:
	black --config pyproject.toml . && isort .

.POHNY: ci-format-checker
ci-format-checker:
	echo "run black"
	black --check --config pyproject.toml . ../example

.POHNY: ci-lint
ci-lint:
	echo "run flake8"
	flake8 . ../example

.POHNY: ci-mypy
ci-mypy:
	echo "run mypy"
	mypy . ../example/mnist

.POHNY: ci-isort
ci-isort:
	echo "run isort"
	isort --check . ../example

.POHNY: ut
ut:
	echo "ut"
	pytest tests -vvrfEsx --cov-config=.coveragerc --cov=starwhale --cov-report=xml:coverage.xml --cov-report=term-missing

.POHNY: all-check
all-check: ci-format-checker ci-lint ci-mypy ci-isort ut
