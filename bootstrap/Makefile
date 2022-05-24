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
	python3 -m pip install -r requirements.txt

ansible-lint:
	ansible-lint

# make check {your admin name}
ansible-check:
	ansible-playbook starwhale/playbook/bootstrap.yaml -i hosts --user $(user) --check

# make play {your admin name}
ansible-play:
	ansible-playbook starwhale/playbook/bootstrap.yaml -i hosts --user $(user)