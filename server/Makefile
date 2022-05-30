build-package:
	mvn -B package -DskipTests
ci-package-with-unit-test:
	mvn -B package jacoco:report coveralls:report -DrepoToken=${COVERALL_TOKEN}