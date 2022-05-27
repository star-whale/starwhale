build-package:
	mvn -B package -DskipTests
ci-package-with-unit-test:
	mvn -B clean test jacoco:report coveralls:report -DrepoToken=${GITHUB_TOKEN}