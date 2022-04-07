check:
	python setup.py check

build-wheel: check
	python setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	python setup.py register sdist upload

build-dev-wheel:
	pip install -e .