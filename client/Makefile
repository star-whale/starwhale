check:
	python setup.py check

clean:
	rm -rf dist/*

build-wheel: check clean
	python setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	python setup.py register sdist upload

build-dev-wheel:
	pip install -e .