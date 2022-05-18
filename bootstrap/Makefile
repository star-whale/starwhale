
install:
	python3 -m pip install -r requirements.txt

lint:
	ansible-lint

# make check user={your admin name}
check:
	ansible-playbook bootstrap.yaml -i hosts --user $(user) --check

# make play user={your admin name}
play:
	ansible-playbook bootstrap.yaml -i hosts --user $(user)