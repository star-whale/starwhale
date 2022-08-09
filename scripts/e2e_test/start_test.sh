#!/usr/bin/env bash

set -e

if [[ ! -z ${DEBUG} ]]; then
    set -x
fi

in_github_action() {
  [ -n "$GITHUB_ACTION" ]
}

file_exists() {
  [ -f "$1" ]
}

if in_github_action; then
    export SW_PYPI_EXTRA_INDEX_URL='https://pypi.org/simple'
else
    SW_PYPI_EXTRA_INDEX_URL='https://pypi.doubanio.com/simple/'
    export PARENT_CLEAN=true
fi

declare_env() {
  export PYPI_RELEASE_VERSION="${PYPI_RELEASE_VERSION:=100.0.0}"
  export RELEASE_VERSION="${RELEASE_VERSION:=0.0.0-dev}"
  export NEXUS_HOSTNAME="${NEXUS_HOSTNAME:=host.minikube.internal}"
  export NEXUS_IMAGE="${NEXUS_IMAGE:=sonatype/nexus3:3.40.1}"
  export NEXUS_USER_NAME="${NEXUS_USER_NAME:=admin}"
  export NEXUS_USER_PWD="${NEXUS_USER_PWD:=admin123}"
  export PORT_NEXUS="${PORT_NEXUS:=8081}"
  export PORT_CONTROLLER="${PORT_CONTROLLER:=8082}"
  export PORT_NEXUS_DOCKER="${PORT_NEXUS_DOCKER:=8083}"
  export IP_MINIKUBE_BRIDGE="${IP_MINIKUBE_BRIDGE:=192.168.49.1}"
  export IP_DOCKER_BRIDGE="${IP_DOCKER_BRIDGE:=172.17.0.1}"
  export IP_MINIKUBE_BRIDGE_RANGE="${IP_MINIKUBE_BRIDGE_RANGE:=192.0.0.0/8}"
  export REPO_NAME_DOCKER="${REPO_NAME_DOCKER:=docker-hosted}"
  export REPO_NAME_PYPI="${REPO_NAME_PYPI:=pypi-hosted}"
  export PYTHON_VERSION="${PYTHON_VERSION:=3.9}"
}

start_minikube() {
  minikube delete
  minikube start --insecure-registry "$IP_MINIKUBE_BRIDGE_RANGE"
  ifconfig
}


start_nexus() {
  docker run -d --publish=$PORT_NEXUS:$PORT_NEXUS --publish=$PORT_NEXUS_DOCKER:$PORT_NEXUS_DOCKER --name nexus  -e NEXUS_SECURITY_RANDOMPASSWORD=false $NEXUS_IMAGE
    sudo cp /etc/hosts /etc/hosts.bak_e2e
    sudo echo "127.0.0.1 $NEXUS_HOSTNAME" | sudo tee -a /etc/hosts
}

build_swcli() {
  if in_github_action; then
      python3 -m pip install --upgrade pip
  else
      python3 -m venv venve2e && . venve2e/bin/activate && python3 -m pip install --upgrade pip
  fi

  pushd ../../client
  python3 -m pip install -r requirements-install.txt
  make build-wheel
  popd
}

build_server_image() {
  pushd ../../server
  make build-package
  pushd ../docker
  docker build -t server -f Dockerfile.server .
  docker tag server $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$PYPI_RELEASE_VERSION
  popd
  popd
}

overwrite_pypirc() {
  if file_exists "$HOME/.pypirc" ; then
    cp $HOME/.pypirc $HOME/.pypirc.bak_e2e
  else
    touch $HOME/.pypirc
  fi
  cat >$HOME/.pypirc << EOF
[distutils]
index-servers =
    nexus

[nexus]
repository =  http://localhost:$PORT_NEXUS/repository/$REPO_NAME_PYPI/
username = $NEXUS_USER_NAME
password = $NEXUS_USER_PWD
EOF

  cat $HOME/.pypirc
}

overwrite_pip_config() {
  if file_exists "$HOME/.pip/pip.conf" ; then
    cp $HOME/.pip/pip.conf $HOME/.pip/pip.conf.bak_e2e
  else
    mkdir -p $HOME/.pip
    touch $HOME/.pip/pip.conf
  fi

  cat >$HOME/.pip/pip.conf << EOF
[global]
index-url = http://localhost:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple
extra-index-url=$SW_PYPI_EXTRA_INDEX_URL

[install]
trusted-host=localhost
EOF

  cat $HOME/.pip/pip.conf
}


create_service_check_file() {
  cp service_wait.sh /tmp/service_wait.sh
}

check_nexus_service() {
  chmod u+x /tmp/service_wait.sh && /tmp/service_wait.sh http://localhost:$PORT_NEXUS
}

create_repository_in_nexus() {
  curl -u $NEXUS_USER_NAME:$NEXUS_USER_PWD -X 'POST' "http://localhost:$PORT_NEXUS/service/rest/v1/repositories/docker/hosted" -H 'accept: application/json' -H 'Content-Type: application/json'  -d "{\"name\":\"$REPO_NAME_DOCKER\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true,\"writePolicy\":\"allow_once\"},\"component\":{\"proprietaryComponents\":true},\"docker\":{\"v1Enabled\":false,\"forceBasicAuth\":false,\"httpPort\":$PORT_NEXUS_DOCKER}}"
  curl -u $NEXUS_USER_NAME:$NEXUS_USER_PWD -X 'PUT' "http://localhost:$PORT_NEXUS/service/rest/v1/security/realms/active"  -H 'accept: application/json' -H 'Content-Type: application/json' -d "[\"DockerToken\",\"NexusAuthenticatingRealm\", \"NexusAuthorizingRealm\"]"
  curl -u $NEXUS_USER_NAME:$NEXUS_USER_PWD -X 'POST' "http://localhost:$PORT_NEXUS/service/rest/v1/repositories/pypi/hosted" -H 'accept: application/json' -H 'Content-Type: application/json' -d "{\"name\":\"$REPO_NAME_PYPI\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true,\"writePolicy\":\"allow_once\"},\"component\":{\"proprietaryComponents\":true}}"

}

upload_pypi_to_nexus() {
  pushd ../../client
  twine upload --repository nexus dist/*
  popd
}

buid_runtime_image() {
  pushd ../../docker
  docker build -t starwhale -f Dockerfile.starwhale --build-arg ENABLE_E2E_TEST_PYPI_REPO=1 --build-arg PORT_NEXUS=$PORT_NEXUS --build-arg LOCAL_PYPI_HOSTNAME=$IP_DOCKER_BRIDGE --build-arg SW_VERSION=$PYPI_RELEASE_VERSION .
  docker tag starwhale $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION
  popd
}

push_images_to_nexus() {
  docker login http://$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER -u $NEXUS_USER_NAME -p $NEXUS_USER_PWD
  docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$PYPI_RELEASE_VERSION
  docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION
}

start_docker_compose() {
  pushd ../../docker/charts
  helm upgrade --install starwhale ./ --namespace starwhale --create-namespace --set "minikube.enabled=true,image.registry=$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER,image.tag=$PYPI_RELEASE_VERSION"
  popd
}

check_controller_service() {
  while true
  do
          if kubectl port-forward --namespace starwhale svc/starwhale-controller 8082:8082; then
                  echo "controller started"
                  break
          else
            echo "controller is starting"
            kubectl get pods --namespace starwhale
          fi
          sleep 3
  done
}

standalone_test() {
  pushd ../../client
  rm -rf build/*
  rm -rf dist/*
  rm -rf .pytest_cache
  rm -rf venv*
  pushd ../
  scripts/run_demo.sh
  export WORK_DIR=`cat WORK_DIR`
  export LOCAL_DATA_DIR=`cat LOCAL_DATA_DIR`
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
  docker kill nexus
  docker container rm nexus
  docker image rm starwhale
  docker image rm $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION
  docker image rm $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$PYPI_RELEASE_VERSION
  docker image rm server
  mv ~/.pypirc.bak_e2e ~/.pypirc
  mv ~/.pip/pip.conf.bak_e2e ~/.pip/pip.conf
  rm /tmp/service_wait.sh
  script_dir="$(dirname -- "$(readlink -f "${BASH_SOURCE[0]}")")"
  helm uninstall starwhale -n starwhale
  cd $script_dir/../../
  WORK_DIR=`cat WORK_DIR`
  if test -n $WORK_DIR ; then
    rm -rf "$WORK_DIR"
  fi
  rm WORK_DIR
  rm LOCAL_DATA_DIR
  echo 'cleanup'
}
#if ! in_github_action; then
#  trap restore_env EXIT
#fi

main() {
  declare_env
  start_nexus
  start_minikube
  overwrite_pip_config
  overwrite_pypirc
  build_swcli
  build_server_image
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
}

main
