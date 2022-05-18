
install:
	pip install -r requirements.txt

lint:
	ansible-lint

# make check {your admin name}
check:
	ansible-playbook bootstrap.yaml -i hosts --user $(1) --check

# make play {your admin name}
play:
	ansible-playbook bootstrap.yaml -i hosts --user $(1)