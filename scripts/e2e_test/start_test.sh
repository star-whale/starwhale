#!/bin/bash -x

in_github_action() {
  [ -n "$GITHUB_ACTION" ]
}

  if in_github_action; then
      export SW_PYPI_EXTRA_INDEX_URL='https://pypi.org/simple'
  else
      SW_PYPI_EXTRA_INDEX_URL='https://pypi.doubanio.com/simple/'
      PARENT_CLEAN=true
  fi

declare_env() {
  export PYPI_RELEASE_VERSION="${PYPI_RELEASE_VERSION:=100.0.0}"
  export RELEASE_VERSION="${RELEASE_VERSION:=0.0.0-dev}"
  export NEXUS_HOSTNAME="${NEXUS_HOSTNAME:=host.nexus}"
  export NEXUS_IMAGE="${NEXUS_IMAGE:=sonatype/nexus3:3.40.1}"
  export NEXUS_USER_NAME="${NEXUS_USER_NAME:=admin}"
  export NEXUS_USER_PWD="${NEXUS_USER_PWD:=admin123}"
  export PORT_NEXUS="${PORT_NEXUS:=8081}"
  export PORT_CONTROLLER="${PORT_CONTROLLER:=8082}"
  export PORT_NEXUS_DOCKER="${PORT_NEXUS_DOCKER:=9001}"
  export IP_DOCKER_COMPOSE_BRIDGE="${IP_DOCKER_COMPOSE_BRIDGE:=172.18.0.1}"
  export SW_IMAGE_REPO="${SW_IMAGE_REPO:=host.nexus:9001}"
  export IP_DOCKER_BRIDGE="${IP_DOCKER_BRIDGE:=172.17.0.1}"
  export IP_DOCKER_COMPOSE_BRIDGE_RANGE="${IP_DOCKER_COMPOSE_BRIDGE_RANGE:=172.0.0.0/8}"
  export REPO_NAME_DOCKER="${REPO_NAME_DOCKER:=docker-hosted}"
  export REPO_NAME_PYPI="${REPO_NAME_PYPI:=pypi-hosted}"
  export PYTHON_VERSION="${PYTHON_VERSION:=3.9}"
}

start_nexus() {
  docker run -d --publish=$PORT_NEXUS:$PORT_NEXUS --publish=$PORT_NEXUS_DOCKER:$PORT_NEXUS_DOCKER --name nexus  -e NEXUS_SECURITY_RANDOMPASSWORD=false $NEXUS_IMAGE
  sudo cp /etc/hosts /etc/hosts.bake2etest
  sudo echo "127.0.0.1 $NEXUS_HOSTNAME" | sudo tee -a /etc/hosts
}

build_swcli() {
  if in_github_action; then
      pip install --upgrade pip
  else
      python3 -m venv venve2e && . venve2e/bin/activate && pip install --upgrade pip
  fi

  pushd ../../client
  pip install -r requirements-install.txt
  make build-wheel
  popd
}

build_server_image() {
  pushd ../../server
  make build-package
  pushd ../docker
  docker build -t server -f Dockerfile.server .
  docker tag server $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/server
  override_docker_compose
  popd
  popd
}

override_docker_compose() {
  cp compose/compose.override.yaml compose/compose.override.yaml.bake2etest
  cat > compose/compose.override.yaml << EOF
services:
  controller:
    image: $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/server

  agent:
    image: $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/server
    environment:
      - SW_PYPI_INDEX_URL=http://$IP_DOCKER_COMPOSE_BRIDGE:$PORT_NEXUS/$REPO_NAME_PYPI/simple
      - SW_PYPI_EXTRA_INDEX_URL=$SW_PYPI_EXTRA_INDEX_URL
      - SW_PYPI_TRUSTED_HOST=$IP_DOCKER_COMPOSE_BRIDGE
      - SW_TASK_USE_HOST_NETWORK=1
    extra_hosts:
      - $NEXUS_HOSTNAME:$IP_DOCKER_COMPOSE_BRIDGE
  taskset:
    volumes:
      - agent_data:/opt/starwhale
      - taskset_dind_data:/var/lib/docker
      - /tmp/docker-daemon.json:/etc/docker/daemon.json
    extra_hosts:
      - $NEXUS_HOSTNAME:$IP_DOCKER_COMPOSE_BRIDGE

EOF

}

overwrite_pypirc() {
  cp ~/.pypirc ~/.pypirc.bake2etest
  cat > ~/.pypirc << EOF
[distutils]
index-servers =
    nexus

[nexus]
repository =  http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/
username = $NEXUS_USER_NAME
password = $NEXUS_USER_PWD
EOF

}

overwrite_pip_config() {
  cp ~/.pip/pip.conf ~/.pip/pip.conf.bake2etest
  cat > ~/.pip/pip.conf << EOF
[global]
index-url = http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple
extra-index-url=$SW_PYPI_EXTRA_INDEX_URL

[install]
trusted-host=$NEXUS_HOSTNAME
EOF

}

create_daemon_json_for_taskset() {
  echo "{\"hosts\":[\"tcp://0.0.0.0:2376\",\"unix:///var/run/docker.sock\"],\"insecure-registries\":[\"10.0.0.0/8\",\"127.0.0.0/8\",\"$IP_DOCKER_COMPOSE_BRIDGE_RANGE\",\"192.0.0.0/8\"],\"live-restore\":true,\"max-concurrent-downloads\":20,\"max-concurrent-uploads\":20,\"registry-mirrors\":[\"http://$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER\"],\"mtu\":1450,\"runtimes\":{\"nvidia\":{\"path\":\"nvidia-container-runtime\",\"runtimeArgs\":[]}},\"storage-driver\":\"overlay2\"}" > /tmp/docker-daemon.json
}


create_service_check_file() {
  cp service_wait.sh /tmp/service_wait.sh
}

check_nexus_service() {
  chmod u+x /tmp/service_wait.sh && /tmp/service_wait.sh http://$NEXUS_HOSTNAME:$PORT_NEXUS
}

create_repository_in_nexus() {
  curl -u $NEXUS_USER_NAME:$NEXUS_USER_PWD -X 'POST' "http://$NEXUS_HOSTNAME:$PORT_NEXUS/service/rest/v1/repositories/docker/hosted" -H 'accept: application/json' -H 'Content-Type: application/json'  -d "{\"name\":\"$REPO_NAME_DOCKER\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true,\"writePolicy\":\"allow_once\"},\"component\":{\"proprietaryComponents\":true},\"docker\":{\"v1Enabled\":false,\"forceBasicAuth\":false,\"httpPort\":$PORT_NEXUS_DOCKER}}"
  curl -u $NEXUS_USER_NAME:$NEXUS_USER_PWD -X 'PUT' "http://$NEXUS_HOSTNAME:$PORT_NEXUS/service/rest/v1/security/realms/active"  -H 'accept: application/json' -H 'Content-Type: application/json' -d "[\"DockerToken\",\"NexusAuthenticatingRealm\", \"NexusAuthorizingRealm\"]"
  curl -u $NEXUS_USER_NAME:$NEXUS_USER_PWD -X 'POST' "http://$NEXUS_HOSTNAME:$PORT_NEXUS/service/rest/v1/repositories/pypi/hosted" -H 'accept: application/json' -H 'Content-Type: application/json' -d "{\"name\":\"$REPO_NAME_PYPI\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true,\"writePolicy\":\"allow_once\"},\"component\":{\"proprietaryComponents\":true}}"

}

upload_pypi_to_nexus() {
  pushd ../../client
  twine upload --repository nexus dist/*
  popd
}

buid_runtime_image() {
  pushd ../../docker
  docker build -t starwhale -f Dockerfile.starwhale --build-arg PIPY_REPO_E2E=true --build-arg PORT_NEXUS=$PORT_NEXUS --build-arg NEXUS_HOSTNAME=$IP_DOCKER_BRIDGE --build-arg SW_VERSION=$PYPI_RELEASE_VERSION .
  docker tag starwhale $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/starwhale:$PYPI_RELEASE_VERSION
  popd
}

push_images_to_nexus() {
  docker login http://$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER -u $NEXUS_USER_NAME -p $NEXUS_USER_PWD
  docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/server
  docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/starwhale:$PYPI_RELEASE_VERSION
}

start_docker_compose() {
  pushd ../../docker/compose
  if ! type "$docker-compose" > /dev/null; then
    docker compose up -d
  else
    docker-compose up -d
  fi
  popd
}

check_controller_service() {
  chmod u+x /tmp/service_wait.sh && /tmp/service_wait.sh http://$NEXUS_HOSTNAME:$PORT_CONTROLLER
}

standalone_test() {
  pushd ../../client
  rm -rf build/*
  rm -rf dist/*
  rm -rf .pytest_cache
  rm -rf venv*
  pushd ../
  scripts/run_demo.sh
  scripts/e2e_test/copy_artifacts_to_server.sh 127.0.0.1:$PORT_CONTROLLER
  scripts/e2e_test/test_job_run.sh 127.0.0.1:$PORT_CONTROLLER
  popd
  popd

}

api_test() {
  pushd ../apitest/pytest
  python3 -m pip install -r requirements.txt
  pytest --host 127.0.0.1 --port $PORT_CONTROLLER
  popd
}

restore_env() {
  rm -rf venve2e
  mv ~/.pypirc.bake2etest ~/.pypirc
  mv ~/.pip/pip.conf.bake2etest ~/.pip/pip.conf
  sudo mv /etc/hosts.bake2etest /etc/hosts
  rm /tmp/service_wait.sh
  docker kill nexus
  docker container rm nexus
  docker image rm starwhale
  docker image rm $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/starwhale:$PYPI_RELEASE_VERSION
  docker image rm $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/server
  docker image rm server
  pushd ../../docker/compose
  mv compose.override.yaml.bake2etest compose.override.yaml
  dc=`which docker-compose`
  if [ -z $dc ]; then
      docker compose down
  else
      docker-compose down
  fi
  popd
  pushd ../../
  WORK_DIR=`cat WORK_DIR`
  if ! in_github_action; then
    rm -rf "$WORK_DIR"
  fi
  echo 'cleanup'
  popd
}

main() {
  declare_env
  start_nexus
  overwrite_pip_config
  overwrite_pypirc
  build_swcli
  build_server_image
  create_daemon_json_for_taskset
  create_service_check_file
  check_nexus_service
  create_repository_in_nexus
  upload_pypi_to_nexus
  buid_runtime_image
  push_images_to_nexus
  start_docker_compose
  check_controller_service
  standalone_test
  api_test
  if ! in_github_action; then
      restore_env
  fi
}

main