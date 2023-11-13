SHELL=/bin/sh
MVN_IMG=maven:3.8.5-openjdk-11
MVN_VOLUME=maven-repo

build-package:
	mvn -B clean package -DskipTests

ci-package-with-unit-test:
	mvn -B -e clean package

ci-package-with-unit-test-report:
	mvn -B -e clean package jacoco:report

show-junit-log:
	find . -type f -path "*sw-test-output/*.txt" -exec tail -n +1 {} +
