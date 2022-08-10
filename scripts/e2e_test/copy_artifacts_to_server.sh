#!/usr/bin/env bash

set -e

if [[ ! -z ${DEBUG} ]]; then
    set -x
fi

source $WORK_DIR/.venv/bin/activate
if [ -z "$GITHUB_ACTION" ]; then
  export SW_CLI_CONFIG="$LOCAL_DATA_DIR/config.yaml"
  export SW_LOCAL_STORAGE=$LOCAL_DATA_DIR/data
fi
swcli instance login http://$1 --username starwhale --password abcd1234 --alias pre-k8s
swcli model copy mnist/version/latest cloud://pre-k8s/project/1
swcli dataset copy mnist/version/latest cloud://pre-k8s/project/1
swcli runtime copy pytorch-mnist/version/latest cloud://pre-k8s/project/1
