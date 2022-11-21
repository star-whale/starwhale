#!/usr/bin/env bash

set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
echo "$SCRIPT_DIR"
pushd ../..
REPO_PATH=$(pwd)
popd
echo "$REPO_PATH"
export WORK_DIR=`mktemp -d -p "$SCRIPT_DIR"`
export SW_CLI_CONFIG="$WORK_DIR/config.yaml"
export SW_LOCAL_STORAGE="$WORK_DIR/data"
# deletes the temp directory
function cleanup {
  rm -rf "$WORK_DIR"
  echo "Deleted temp working directory $WORK_DIR"
}
trap cleanup EXIT
rsync -av $REPO_PATH/client $WORK_DIR --exclude venv --exclude .venv
cp $REPO_PATH/README.md $WORK_DIR
pushd $WORK_DIR
python3 -m venv venve2e && . venve2e/bin/activate && python3 -m pip install --upgrade pip
if [[ -n $PYPI_RELEASE_VERSION ]] ; then
  python3 -m pip install starwhale==$PYPI_RELEASE_VERSION
else
  python3 -m pip install -e client
fi

swcli --version
popd
python3 cli_test.py "$1"

