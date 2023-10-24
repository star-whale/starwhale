#!/usr/bin/env bash

set -x
export WORK_DIR=`mktemp -d`
function cleanup {
  rm -rf "$WORK_DIR"
  echo "Deleted temp working directory $WORK_DIR"
}
trap cleanup EXIT
pushd $WORK_DIR
export SW_LOCAL_STORAGE=$WORK_DIR
python3 -m venv venv && . venv/bin/activate && python3 -m pip install --upgrade pip
pip install starwhale==$PYPI_RELEASE_VERSION
swcli --version
swcli instance list
swcli ds list
swcli dataset list
swcli mp list
swcli model list
swcli rt list
swcli runtime list
popd
