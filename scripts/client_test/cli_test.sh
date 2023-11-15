#!/usr/bin/env bash

set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
REPO_PATH=$( cd -- "$SCRIPT_DIR/../.." &> /dev/null && pwd )
export WORK_DIR=`mktemp -d`
function cleanup {
  rm -rf "$WORK_DIR"
  echo "Deleted temp working directory $WORK_DIR"
}
trap cleanup EXIT
pushd $WORK_DIR
python3 -m venv venv && . venv/bin/activate && python3 -m pip install --upgrade pip
python3 -m pip install -r "$SCRIPT_DIR"/requirements.txt
if [[ -n $PYPI_RELEASE_VERSION ]] ; then
  python3 -m pip install starwhale==$PYPI_RELEASE_VERSION
else
  rm -rf ${REPO_PATH}/client/dist
  python3 -m pip install -e $REPO_PATH/client
fi
swcli --version

ls -lah ${REPO_PATH}/client/dist

popd

bash "$SCRIPT_DIR"/update_controller_setting.sh
for i in $@; do
    python3 "$SCRIPT_DIR"/cli_test.py --sw_repo_path "$REPO_PATH" --case $i --work_dir "$WORK_DIR" --server_url "$CONTROLLER_URL" || exit 1
done

SW_CLI_CONFIG="$WORK_DIR"/config.yaml . "$SCRIPT_DIR"/cli_exec.sh
