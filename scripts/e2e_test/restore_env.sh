#!/bin/bash -x

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
}

restore_env