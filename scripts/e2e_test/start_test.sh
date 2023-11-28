#!/usr/bin/env bash

set -x

if [[ -n ${DEBUG} ]]; then
    set -e
fi

in_github_action() {
  [ -n "$GITHUB_ACTION" ]
}

file_exists() {
  [ -f "$1" ]
}

use_docker_compose() {
  [ "$SERVER_DRIVER" == "docker-compose" ]
}

export SWNAME="${SWNAME:=e2e}"
export SWNS="${SWNS:=e2e}"

if in_github_action; then
    export SW_PYPI_EXTRA_INDEX_URL="${SW_PYPI_EXTRA_INDEX_URL:=https://pypi.org/simple}"
    if use_docker_compose; then
        export CONTROLLER_HOST=127.0.0.1
        export CONTROLLER_PORT=8082
    else
        export CONTROLLER_HOST=controller.$SWNS.svc
        export CONTROLLER_PORT=80
    fi
    export MINIO_HOST=minio.$SWNS.svc
    export MINIO_ADMIN_HOST=minio-admin.$SWNS.svc
    export CONTROLLER_URL=http://${CONTROLLER_HOST}:${CONTROLLER_PORT}
else
    export SW_PYPI_EXTRA_INDEX_URL="${SW_PYPI_EXTRA_INDEX_URL:=https://pypi.doubanio.com/simple}"
    export PARENT_CLEAN="${PARENT_CLEAN:=true}"
    export CONTROLLER_HOST=${SWNAME//./-}.pre.intra.starwhale.ai
    export MINIO_HOST=${SWNAME//./-}-minio.pre.intra.starwhale.ai
    export MINIO_ADMIN_HOST=${SWNAME//./-}-minio-admin.pre.intra.starwhale.ai
    export CONTROLLER_URL=http://${CONTROLLER_HOST}
fi

declare_env() {
  export PYPI_RELEASE_VERSION="${PYPI_RELEASE_VERSION:=100.0.0}"
  export SERVER_RELEASE_VERSION="${SERVER_RELEASE_VERSION:=100.0.0}"
  export RELEASE_VERSION="${RELEASE_VERSION:=0.0.0-dev}"
  export NEXUS_HOSTNAME="${NEXUS_HOSTNAME:=host.minikube.internal}"
  export NEXUS_IMAGE="${NEXUS_IMAGE:=sonatype/nexus3:3.40.1}"
  export NEXUS_USER_NAME="${NEXUS_USER_NAME:=admin}"
  export NEXUS_USER_PWD="${NEXUS_USER_PWD:=admin123}"
  export PORT_NEXUS="${PORT_NEXUS:=8081}"
  export PORT_NEXUS_DOCKER="${PORT_NEXUS_DOCKER:=8083}"
  export IP_MINIKUBE_BRIDGE="${IP_MINIKUBE_BRIDGE:=192.168.49.1}"
  # export SW_IMAGE_REPO="${SW_IMAGE_REPO:=$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER}"
  export IP_MINIKUBE_BRIDGE_RANGE="${IP_MINIKUBE_BRIDGE_RANGE:=192.0.0.0/8}"
  export REPO_NAME_DOCKER="${REPO_NAME_DOCKER:=docker-hosted}"
  export REPO_NAME_PYPI="${REPO_NAME_PYPI:=pypi-hosted}"
  export PYTHON_VERSION="${PYTHON_VERSION:=3.9}"
}

start_minikube() {
    minikube start -p e2e --memory=6G --insecure-registry "$IP_MINIKUBE_BRIDGE_RANGE" --driver=docker --container-runtime=docker
    minikube addons enable ingress -p e2e
    minikube addons enable ingress-dns -p e2e
    minikube -p e2e docker-env
}

show_k8s_logs() {
    echo "--> show jobs logs:"
    kubectl -n $SWNS get jobs -o wide
    kubectl -n $SWNS describe jobs
    kubectl -n $SWNS logs --tail 10000 -l job-name --all-containers --prefix

    echo "--> show controller logs:"
    kubectl -n $SWNS get deployments.apps -o wide
    kubectl -n $SWNS describe deployments.apps controller
    kubectl -n $SWNS logs --tail 10000 deployment/controller --all-containers --prefix
}

show_docker_compose_logs() {
    echo "--> show all containers log:"
    docker compose -f ~/.starwhale/.server/docker-compose.yaml logs
    docker ps --format '{{.Names}}' -a | grep 'starwhale-run' | xargs -L 1 docker logs || true
    docker ps -a
    docker images -a
}

show_logs() {
    if use_docker_compose; then
        show_docker_compose_logs
    else
        show_k8s_logs
    fi
}

start_nexus() {
  docker run -d --publish=$PORT_NEXUS:$PORT_NEXUS --publish=$PORT_NEXUS_DOCKER:$PORT_NEXUS_DOCKER --name nexus  -e NEXUS_SECURITY_RANDOMPASSWORD=false $NEXUS_IMAGE
  sudo cp /etc/hosts /etc/hosts.bak_e2e
  echo "127.0.0.1 $NEXUS_HOSTNAME" | sudo tee -a /etc/hosts
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

build_console() {
  pushd ../../console
  mkdir build
  echo 'hi' > build/index.html
  popd
}

build_server_image() {
  pushd ../../server
  make build-package
  popd
  pushd ../../docker
  docker build \
    --build-arg BASE_IMAGE=ghcr.io/star-whale/base_server:latest \
    --tag $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION \
    -f Dockerfile.server .
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
repository =  http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/
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
index-url = http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple
extra-index-url=$SW_PYPI_EXTRA_INDEX_URL

[install]
trusted-host=$NEXUS_HOSTNAME
EOF

  cat $HOME/.pip/pip.conf
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

push_images_to_nexus() {
    push_server_image_to_nexus
}

push_server_image_to_nexus() {
  docker login http://$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER -u $NEXUS_USER_NAME -p $NEXUS_USER_PWD
  docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION
}

start_starwhale() {
  if use_docker_compose; then
    start_starwhale_by_docker_compose
  else
    start_starwhale_by_helm
  fi
}

start_starwhale_by_docker_compose() {
    python3 -m pip install ../../client/dist/starwhale-100.0.0-py3-none-any.whl
    swcli server start \
        --host 127.0.0.1 \
        --port 8082 \
        --server-image $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION \
        --db-image ghcr.io/star-whale/bitnami-mysql:8.0.29-debian-10-r2 \
        --env SW_PYPI_INDEX_URL=http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple \
        --env SW_PYPI_EXTRA_INDEX_URL=$SW_PYPI_EXTRA_INDEX_URL \
        --env SW_PYPI_TRUSTED_HOST=$NEXUS_HOSTNAME
    docker network connect starwhale_local_ns nexus --alias $NEXUS_HOSTNAME
}

start_starwhale_by_helm() {
  pushd ../../docker/charts
  helm upgrade --install $SWNAME --namespace $SWNS --create-namespace \
  --set resources.controller.requests.cpu=700m \
  --set mysql.resources.requests.cpu=300m \
  --set minio.resources.requests.cpu=200m \
  --set minikube.enabled=true \
  --set image.registry=$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER \
  --set image.server.tag=$SERVER_RELEASE_VERSION \
  --set mirror.pypi.enabled=true \
  --set mirror.pypi.indexUrl=http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple \
  --set mirror.pypi.extraIndexUrl=$SW_PYPI_EXTRA_INDEX_URL \
  --set mirror.pypi.trustedHost=$NEXUS_HOSTNAME \
  --set controller.ingress.enabled=true \
  --set controller.ingress.host=$CONTROLLER_HOST \
  --set minio.ingress.enabled=true \
  --set minio.ingress.host=$MINIO_HOST \
  --set minio.ingress.admin_host=$MINIO_ADMIN_HOST \
  --set minio.ports.api=80 .
  # workaround(minio 80 port) for internal and external use the same minio host: minio.starwhale.svc
  popd
}

check_services_alive() {
    if use_docker_compose; then
        check_services_alive_by_docker_compose
    else
        check_services_alive_by_k8s
    fi

    bash service_wait.sh ${CONTROLLER_URL}
    echo "controller started"
}

check_services_alive_by_docker_compose() {
    while true
    do
      state=`docker ps --filter name=starwhale_local-server-1 --format "{{.State}}"`
      if [[ "$state" == "running" ]]; then
        echo "server ready"
        break
      else
        echo "server is starting"
        show_docker_compose_logs
      fi
      sleep 5
    done
}

check_services_alive_by_k8s() {
    while true
    do
      ready=`kubectl get pod -l starwhale.ai/role=controller -n $SWNS -o json| jq -r '.items[0].status.containerStatuses[0].ready'`
      if [[ "$ready" == "true" ]]; then
        echo "controller ready"
        break
      else
        echo "controller is starting"
        kubectl -n $SWNS get pods
        kubectl -n $SWNS describe deployments/controller || true
        kubectl -n $SWNS logs --tail 500 deployments/controller || true
      fi

      sleep 5
    done
}

setup_minikube_dns_mock() {
    DNS_RECORD="$(minikube ip) ${MINIO_HOST} ${CONTROLLER_HOST} ${MINIO_ADMIN_HOST}"
    if ! fgrep "$DNS_RECORD" /etc/hosts; then
      echo "$DNS_RECORD" | sudo tee -a /etc/hosts
    fi
}

open_api_model_test() {
    # generate openapi model
    pushd ../../client
    python3 -m pip install datamodel-code-generator[http]
    OPEN_API_URL=$CONTROLLER_URL make gen-model || exit 1

    if git diff --exit-code; then
      echo "openapi model is up to date"
    else
      echo "openapi model is not up to date"
      git diff
      exit 1
    fi
    popd
}

client_test() {
  pushd ../../client
  rm -rf build/*
  rm -rf dist/*
  rm -rf .pytest_cache
  rm -rf venv*
  pushd ../
  python3 -m venv .venv && . .venv/bin/activate && pip install --upgrade pip
  if ! in_github_action; then
    unset http_proxy
    unset https_proxy
    bash scripts/client_test/cli_test.sh all
  else
    timeout 20m bash scripts/client_test/cli_test.sh simple || exit 1
  fi
  popd
  popd
}

api_test() {
  pushd ../apitest/pytest
  python3 -m pip install -r requirements.txt
  pytest --host ${CONTROLLER_HOST} --port ${CONTROLLER_PORT} || exit 1
  popd
  # if ! in_github_action; then
  #   source upgrade_test.sh
  # fi
}

console_test() {
  if ! in_github_action; then
    docker run --rm --ipc=host -w /app -e PROXY=${CONTROLLER_URL} -v $SWROOT/console/playwright:/app mcr.microsoft.com/playwright:v1.33.0-jammy /bin/bash -c "yarn && yarn test" || exit 1
  fi
}

restore_env() {
  docker image rm $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION
  script_dir="$(dirname -- "$(readlink -f "${BASH_SOURCE[0]}")")"
  cd $script_dir/../../
  WORK_DIR=`cat WORK_DIR`
  if test -n $WORK_DIR ; then
    rm -rf "$WORK_DIR"
  fi
  rm WORK_DIR
  rm LOCAL_DATA_DIR
  echo 'cleanup'
}

exit_hook() {
  if restore_env ; then echo "restore_env success" ; fi
}

publish_to_mini_k8s() {
  start_nexus
  start_minikube
  overwrite_pip_config
  overwrite_pypirc
  build_swcli
  build_console
  build_server_image
  create_service_check_file
  check_nexus_service
  create_repository_in_nexus
  upload_pypi_to_nexus
  push_images_to_nexus
  start_starwhale
  check_services_alive
  setup_minikube_dns_mock
}

publish_to_k8s() {
  pushd ../publish
  bash pub.sh --config
  source pub.sh all -s --app $SWNAME --ns $SWNS
  popd
}

main() {
  declare_env
  if ! in_github_action; then
    trap exit_hook EXIT
    publish_to_k8s
    sleep 120
    check_services_alive
  else
    publish_to_mini_k8s
  fi
  client_test
  open_api_model_test
  api_test
  console_test
}

declare_env
if test -z $1; then
  main
else
  $1
fi
