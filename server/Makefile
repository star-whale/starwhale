SHELL=/bin/sh
MVN_IMG=maven:3.8.5-openjdk-11
MVN_VOLUME=maven-repo

build-package:
	mvn -B clean package -DskipTests

ci-package-with-unit-test:
	mvn -B -e clean package

ci-package-with-unit-test-report:
	mvn -B -e clean package jacoco:report

test:
	docker volume create --name ${MVN_VOLUME} && \
	docker run --rm -v ${MVN_VOLUME}:/app ${MVN_IMG} /bin/sh -c "chown $(shell id -u):$(shell id -g) -R /app" && \
	docker run --rm -u $(shell id -u):$(shell id -g) \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		-v ${MVN_VOLUME}:/var/maven/.m2 \
		-v $(shell pwd):/app \
		-w /app ${MVN_IMG} \
		mvn -Duser.home=/var/maven -e test
