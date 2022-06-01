PY_CHANGED_FILES = $(shell git diff --name-only --relative -- '*.py')

check:
	python3 setup.py check

clean:
	rm -rf dist/*

build-wheel: check clean
	python3 setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	twine upload dist/*

install-sw:
	python3 -m pip install -e .

install-dev-req:
	python3 -m pip install -r requirements-dev.txt

black-format:
	black --config pyproject.toml $(PY_CHANGED_FILES)

isort-format:
	isort $(PY_CHANGED_FILES)

ci-format-checker:
	echo "run black"
	black --check --config pyproject.toml .

ci-lint:
	echo "run flake8"
	flake8 .

ci-mypy:
	echo "run mypy"
	mypy --python-version=3.7 .

ci-isort:
	echo "run isort"
	isort --check .

ut:
	echo "ut"
	pytest tests -vvrfEsx --cov-config=.coveragerc --cov=starwhale --cov-report=xml:coverage.xml --cov-report=term-missing

all-check: ci-format-checker ci-lint ci-mypy ci-isort ut