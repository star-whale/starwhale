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

echo $WORK_DIR > WORK_DIR

finish() {
  if ! in_github_action && test -z $PARENT_CLEAN ; then
    rm -rf "$WORK_DIR"
  fi
  echo 'cleanup'
}

trap finish EXIT


if ! in_github_action; then
  cp -rf ./client "$WORK_DIR"
  cp -rf ./example "$WORK_DIR"
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
swcli runtime quickstart . --python-env=venv --create-env --name pytorch-mnist
# shellcheck source=/dev/null
. .venv/bin/activate

python3 -m pip install -r requirements.txt

echo "install swcli for mnist venv"
pushd ../../client
make install-sw
popd

prepare_data() {
  mkdir -p data && pushd data
  if [ ! -f train-images-idx3-ubyte ]; then
    wget http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz
    wget http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz
    wget http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
    wget http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
    gzip -d -- *.gz
  fi
  popd
}

prepare_data

must_equal() {
  if [ "$1" != "$2" ]; then
    echo "$3: expect $1, get $2"
    exit 1
  fi
}

length_must_equal() {
  must_equal "$1" "$(swcli -o json "$2" list | jq length)" "$2 list"
}

build_rc_and_check() {
  length_must_equal 0 "$1"
  swcli "$1" build .
  length_must_equal 1 "$1"
}

build_rc_and_check runtime
build_rc_and_check model
build_rc_and_check dataset

echo "do ppl and cmp"
length_must_equal 0 job "job list"
swcli job create --model mnist/version/latest --dataset mnist/version/latest
length_must_equal 1 job "job list"

echo "check result"
accuracy=$(swcli -o json job info "$(swcli -o json job list | jq -r '.[0].manifest.version')" | jq '.report.summary.accuracy')
must_equal 0.9894 "$accuracy" "mnist accuracy"
echo "done"
