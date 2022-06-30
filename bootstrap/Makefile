PY_CHANGED_FILES = $(shell git diff --name-only --relative -- '*.py')

check:
	python3 setup.py check

clean:
	rm -rf dist/*

build-wheel: check clean
	python3 setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	python3 -m twine upload --repository pypi dist/* --verbose

install-dev:
	python3 -m pip install -r requirements-dev.txt

isort-format:
	isort $(PY_CHANGED_FILES)

black-format:
	black --config pyproject.toml $(PY_CHANGED_FILES)

ci-format-checker:
	echo "run black"
	black --check --config pyproject.toml .

ci-lint:
	echo "run flake8"
	flake8 .

ci-isort:
	echo "run isort"
	isort --check .

all-check: ci-format-checker ci-lint

ansible-lint:
	ansible-lint