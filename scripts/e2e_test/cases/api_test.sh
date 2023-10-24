#!/usr/bin/env bash

set -x
export CONTROLLER_HOST=${SWNAME//./-}.pre.intra.starwhale.ai
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
REPO_PATH=$( cd -- "$SCRIPT_DIR/../../.." &> /dev/null && pwd )
export WORK_DIR=`mktemp -d`
function cleanup {
  rm -rf "$WORK_DIR"
  echo "Deleted temp working directory $WORK_DIR"
}
trap cleanup EXIT
pushd $WORK_DIR
python3 -m venv venv && . venv/bin/activate && python3 -m pip install --upgrade pip
popd
pushd "$REPO_PATH"/scripts/apitest/pytest
python3 -m pip install -r requirements.txt
pytest --host ${CONTROLLER_HOST} --port 80 || exit 1
popd