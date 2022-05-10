check:
	python3 setup.py check

clean:
	rm -rf dist/*

build-wheel: check clean
	python3 setup.py sdist bdist_wheel
	ls -alh dist

upload-pypi:
	twine upload dist/*

build-dev-wheel:
	python3 -m pip install -e .

dev-dep:
	python3 -m pip install -r requirements-dev.txt

ci-format:
	echo "ci format"

ci-lint:
	echo "ci lint"

ci-type:
	echo "ci type"

ut:
	echo "ut"

install-req:
	python3 -m pip install .