#! /bin/bash

set -e

PYTHON_VERSION="${PYTHON_VERSION:=3.7}"

in_github_action() {
  [ -n "$GITHUB_ACTION" ]
}

script_dir="$(dirname -- "$(readlink -f "${BASH_SOURCE[0]}")")"
cd "$script_dir"/..
if in_github_action; then
  WORK_DIR="$(pwd)"
else
  WORK_DIR=$(mktemp -d)
  echo "use $(pwd) as source"
fi

export SW_WORK_DIR=$WORK_DIR
echo $WORK_DIR > WORK_DIR

finish() {
  if ! in_github_action && test -z "$PARENT_CLEAN" ; then
    echo 'cleanup work dir '"$WORK_DIR"
    rm -rf "$WORK_DIR"
  fi
  echo 'cleanup'
}

trap finish EXIT


if ! in_github_action; then
  cp -rf ./client "$WORK_DIR"
  cp -rf ./example "$WORK_DIR"
  cp -rf ./scripts "$WORK_DIR"
  cp -rf ./README.md "$WORK_DIR"
  rm -rf "$WORK_DIR/.venv"
  rm -rf "$WORK_DIR/example/mnist/.venv"
  rm -f "$WORK_DIR/example/mnist/runtime.yaml"

  # use a separate data & config dir
  LOCAL_DATA_DIR=$(mktemp -d -p $WORK_DIR)
  export SW_CLI_CONFIG="$LOCAL_DATA_DIR/config.yaml"
  export SW_LOCAL_STORAGE=$LOCAL_DATA_DIR/data
  echo $LOCAL_DATA_DIR > LOCAL_DATA_DIR
fi

echo "start test in $WORK_DIR"
cd "$WORK_DIR" || exit
# shellcheck source=/dev/null
python3 -m venv .venv && . .venv/bin/activate && pip install --upgrade pip
cd "$WORK_DIR/client" || exit

echo "install swcli"
make install-sw

cd "$WORK_DIR/example/mnist" || exit
swcli runtime quickstart shell . --python-env=venv --create-env --name pytorch-mnist
# shellcheck source=/dev/null
. .venv/bin/activate

echo "execute test for mnist example"
pushd ../../scripts/client_test
python3 -m pip install -r requirements.txt
swcli --version
pytest cli_test.py
popd

echo "done"
