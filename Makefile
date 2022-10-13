client-check-all:
	cd client && $(MAKE) all-check
	bash scripts/run_demo.sh
