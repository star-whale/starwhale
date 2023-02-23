#!/usr/bin/env bash

set -e

if [[ -n ${DEBUG} ]]; then
    set -x
fi

--help() {
  echo "Usage: pub.sh [OPTIONS] OBJECT [ARGS]..."
  echo "Options:"
  echo "  --help        Show this message and exit."
  echo "  --config      config will overwrite your local $HOME/.pypirc and $HOME/.pip/pip.conf and do a docker login to nexus so that following function will work"
  echo "OBJECT:"
  echo "  cli           Publish cli pypi version to nexus & publish runtime base image to nexus"
  echo "  console       Publish console to k8s"
  echo "  controller    Update controller to k8s only in dev use case"
  echo "  all           Publish both cli and console"
}

check_git_status() {
  git_status=$(git status -s -uno)
  if test -n "$git_status"; then echo "please commit your local changes, so that we could get a rollback point" && exit 1; fi
}

set_up_version() {
  latest_pr=$(git log | grep -o '(#[0-9]\+)' | head -1 | grep -o '[0-9]\+')
  last_commit_timestamp=$(git log -n1 --format="%at")
  last_commit_time=$(date -d @$last_commit_timestamp +"%m%d%H%M%S")
  release=$(git describe --tags --abbrev=0)
  commit_id=$(git rev-parse --short HEAD)
  branch=$(git branch --show-current)
  pypi_version="$release""dev""$last_commit_time"
  FMT_VERSION_CODE="import pkg_resources; _v=pkg_resources.parse_version('${pypi_version}'); print(_v.public)"
  export PYPI_RELEASE_VERSION=$(python3 -c "${FMT_VERSION_CODE}")
  export SERVER_RELEASE_VERSION="${branch////-}""-""$commit_id"
  export SWNAME=$SERVER_RELEASE_VERSION
  export SWNS=$(whoami)
}

load_config() {
  if test -f ~/.sw/setup.env; then
    export $(grep -v '^#' ~/.sw/setup.env | xargs -0)
  fi
}

file_exists() {
  [ -f "$1" ]
}

build() {

  console() {
    if [[ -n ${DOCKER_BUILD} ]]; then
        pushd ../../docker
        make build-console
        popd
    else
        pushd ../../console
        make install-dev-tools && make build-all
        popd
    fi
    b_controller
  }

  b_controller() {
    if [[ -n ${DOCKER_BUILD} ]]; then
        pushd ../../docker
        make build-jar
        popd
    else
        pushd ../../server
        make build-package
        popd
    fi
    pushd ../../docker
    make build-server
    if ! docker tag starwhaleai/server:latest $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION ; then echo "[ERROR] Something wrong while pushing , press CTL+C to interrupt execution if needed"; fi
    if ! docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION ; then echo "[ERROR] Something wrong while pushing , press CTL+C to interrupt execution if needed"; fi
    popd

  }
  if test -z "$1"; then
    console
  else
    $1
  fi
}

deploy() {
  pushd ../../docker/charts
  helm upgrade --install ${SWNAME//./-} . -n $SWNS --create-namespace \
    --set image.registry=$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER \
    --set image.tag=$SERVER_RELEASE_VERSION \
    --set mirror.pypi.enabled=true \
    --set mirror.pypi.indexUrl=http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple \
    --set mirror.pypi.extraIndexUrl=$SW_PYPI_EXTRA_INDEX_URL \
    --set mirror.pypi.trustedHost=$NEXUS_HOSTNAME \
    --set mysql.image=docker-registry.starwhale.cn/bitnami/mysql:8.0.29-debian-10-r2 \
    --set mysql.initImage=docker-registry.starwhale.cn/bitnami/bitnami-shell:11-debian-11-r6 \
    --set minio.initImage=docker-registry.starwhale.cn/bitnami/bitnami-shell:11-debian-11-r6 \
    --set minio.image=docker-registry.starwhale.cn/bitnami/minio:2022.6.20-debian-11-r0 \
    --set devMode.createPV.enabled=true \
    --set devMode.createPV.host=host005-bj01 \
    --set resources.controller.limits.memory=16G \
    --set devMode.createPV.rootPath=/mnt/data/starwhale/$SWNS/$SWNAME \
    --set minio.ingress.host=${SWNAME//./-}-minio.pre.intra.starwhale.ai \
    --set controller.ingress.host=${SWNAME//./-}.pre.intra.starwhale.ai
  popd
}

--config() {
  --help() {
    echo "Usage: config will overwrite your local $HOME/.pypirc and $HOME/.pip/pip.conf and do a docker login to nexus"
  }
  overwrite_pypirc() {
    if file_exists "$HOME/.pypirc"; then
      echo "[WARNING] this script is modifying $HOME/.pypirc , please double check if it overwrites something wrong!"
      echo "$HOME/.pypirc is backup to $HOME/.pypirc.bak_sw_setup"
      cp $HOME/.pypirc $HOME/.pypirc.bak_sw_setup
    else
      touch $HOME/.pypirc
    fi
    cat >$HOME/.pypirc <<EOF
  [distutils]
  index-servers =
      nexus

  [nexus]
  repository =  http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/
  username = $NEXUS_USER_NAME
  password = $NEXUS_USER_PWD
  # shellcheck disable=SC1039
EOF

    echo "$HOME/.pypirc is modified to "
    cat $HOME/.pypirc
  }

  overwrite_pip_config() {
    if file_exists "$HOME/.pip/pip.conf"; then
      echo "[WARNING] this script is modifying $HOME/.pip/pip.conf , please double check if it overwrites something wrong!"
      echo "$HOME/.pip/pip.conf is backup to $HOME/.pip/pip.conf.bak_sw_setup"
      cp $HOME/.pip/pip.conf $HOME/.pip/pip.conf.bak_sw_setup
    else
      mkdir -p $HOME/.pip
      touch $HOME/.pip/pip.conf
    fi

    cat >$HOME/.pip/pip.conf <<EOF
[global]
timeout = 600
index-url=http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple
extra-index-url=$SW_PYPI_EXTRA_INDEX_URL

[install]
trusted-host=$NEXUS_HOSTNAME
EOF

    echo "$HOME/.pip/pip.conf is modified to "
    cat $HOME/.pip/pip.conf
  }
  load_config
  overwrite_pip_config
  overwrite_pypirc
  docker login http://$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER -u $NEXUS_USER_NAME -p $NEXUS_USER_PWD
  echo "env config done"
}

cli() {
  --help() {
    echo "Usage: Publish cli pypi version to nexus from your code & publish runtime base image to nexus from your code. python virtual environment is recommended as an exe context."
    echo "Options:"
    echo "  --help       Show this message and exit."
    echo "  -v           Show version to be published."
    echo "  -s           Start to publish."
  }
  -v() {
    echo "pypi version used is $PYPI_RELEASE_VERSION"
    echo "pypi repo used is http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple"
    echo "runtime base image  used is $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION"
    echo "run docker to try swcli you just built: docker run -it --entrypoint /bin/bash $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION"
    echo "do not forget to set env : export SW_IMAGE_REPO=$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER before you use swcli"
  }
  -s() {
    pushd ../../client
    python3 -m pip install -r requirements-install.txt
    make build-wheel
    if ! twine upload --repository nexus dist/* ; then echo "[ERROR] Something wrong while uploading pypi version , press CTL+C to interrupt execution if needed"; fi
    popd
    pushd ../../docker
    docker build --network=host -t starwhale -f Dockerfile.starwhale --build-arg ENABLE_E2E_TEST_PYPI_REPO=1 --build-arg PORT_NEXUS=$PORT_NEXUS --build-arg LOCAL_PYPI_HOSTNAME=$NEXUS_HOSTNAME --build-arg SW_VERSION=$PYPI_RELEASE_VERSION  --build-arg SW_PYPI_EXTRA_INDEX_URL="$SW_PYPI_EXTRA_INDEX_URL" .
    docker tag starwhale $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION
    docker tag starwhale $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/starwhale:$PYPI_RELEASE_VERSION
    if ! docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/starwhale:$PYPI_RELEASE_VERSION ; then echo "[ERROR] Something wrong while pushing , press CTL+C to interrupt execution if needed"; fi
    if ! docker push $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/starwhale:$PYPI_RELEASE_VERSION ; then echo "[ERROR] Something wrong while pushing , press CTL+C to interrupt execution if needed"; fi
    popd
    -v
  }

  if test -z "$1"; then
    --help
  else
    load_config
    check_git_status
    set_up_version
    $1
  fi
}
console() {
  --help() {
    echo "Usage: publish a console version to K8S from your code."
    echo "Options:"
    echo "  --help            Show this message and exit."
    echo "  --uninstall       Uninstall console."
    echo "  -v                Show version to be published."
    echo "  -s                Start to publish. You could use --app to fix your app name and --ns to fix your namespace in K8S"
    echo "                    [--app Recommended when in dev use case]"
    echo "                    [--app NOT Recommended when in verify use case]"
  }
  -v() {
    echo "k8s app name used is $SWNAME"
    echo "k8s namespace used is $SWNS"
    echo "server image used is $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION"
  }
  -s() {
    --app() {
      export SWNAME=$1
    }
    --ns() {
      export SWNS=$1
    }
    check_git_status
    if test -n "$1"; then
      $1 "$2"
      if test -n "$3"; then $3 "$4"; fi
    fi
    build console
    deploy
    -v
  }

  --uninstall() {
    helm uninstall $SWNAME -n $SWNS
  }
  if test -z "$1"; then
    --help
  else
    export func=dev
    load_config
    set_up_version
    $1 "$2" "$3"  "$4" "$5"
  fi
}

controller() {
  --help() {
    echo "Usage: only update server code to K8S from your code."
    echo "Cavit: you MUST have your console built before using this function so that console could show properly."
    echo "       This function is Recommended when in dev use case, NOT Recommended when in verify use case"
    echo "Options:"
    echo "  --help       Show this message and exit."
    echo "  -v           Show version to be published."
    echo "  -s           Start to publish. You could use --app to fix your app name and --ns to fix your namespace in K8S"
    echo "               [--app Recommended when in dev use case]"
    echo "               [--app NOT Recommended when in verify use case]"
  }
  -v() {
    echo "k8s app name used is $SWNAME"
    echo "k8s namespace used is $SWNS"
    echo "server image used is $NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION"
  }
  -s() {
    --app() {
      export SWNAME=$1
    }
    --ns() {
      export SWNS=$1
    }

    if test -n "$1"; then
      $1 "$2"
      if test -n "$3"; then $3 "$4"; fi
    fi
    build b_controller
    deploy
    -v
  }

  if test -z "$1"; then
    --help
  else
    load_config
    check_git_status
    set_up_version
    $1 "$2" "$3" "$4" "$5"
  fi
}
all() {
  cli "$1"
  console "$1" "$2" "$3" "$4" "$5" "$6"
}
if test -z "$1"; then
  --help
else
  $1 "$2" "$3" "$4" "$5" "$6"
fi
