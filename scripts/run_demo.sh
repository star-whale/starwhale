#! /bin/bash

set -e

in_github_action() {
  [ -n "$GITHUB_ACTION" ]
}

if in_github_action; then
  WORK_DIR="$(pwd)"
else
  WORK_DIR=$(mktemp -d)
  script_dir="$(dirname -- "$(readlink -f "${BASH_SOURCE[0]}")")"
  cd "$script_dir"/..
  echo "use $(pwd) as source"
fi

finish() {
  if ! in_github_action; then
    rm -rf "$WORK_DIR"
  fi
  echo 'cleanup'
}

trap finish EXIT

clean_user_data() {
	rm -f "$HOME/.config/starwhale/config.yaml"
	rm -rf "$HOME/.cache/starwhale"
}

if ! in_github_action; then
  read -r -p "This script will remove all local data below:
  $HOME/.cache/starwhale
  $HOME/.config/starwhale/config.yaml
Do you want to proceed? (yes/no) " yn
  case $yn in
    yes ) clean_user_data;;
    no ) exit;;
    * ) exit 1;;
  esac
fi

if ! in_github_action; then
  cp -rf . "$WORK_DIR/"
  rm -rf "$WORK_DIR/venv"
  rm -rf "$WORK_DIR/example/mnist/venv"
  rm -f "$WORK_DIR/example/mnist/runtime.yaml"
fi

echo "start test in $WORK_DIR"
cd "$WORK_DIR" || exit
# shellcheck source=/dev/null
python3 -m venv venv && . venv/bin/activate && pip install --upgrade pip
cd "$WORK_DIR/client" || exit

echo "install swcli"
make install-sw

cd "$WORK_DIR/example/mnist" || exit
swcli runtime create -n pytorch-mnist -m venv --python 3.7 .
# shellcheck source=/dev/null
. venv/bin/activate

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
