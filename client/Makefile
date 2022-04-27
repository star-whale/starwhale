check:
	python setup.py check

clean:
	rm -rf dist/*

build-wheel: check clean
	python setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	twine upload dist/*

build-dev-wheel:
	pip install -e .