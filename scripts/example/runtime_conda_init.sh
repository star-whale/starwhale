#!/usr/bin/env bash
# $1 is the client repo path

set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
if [ -z "$1" ];then
    echo "client repo path is not passed as the first arg for this script"
    exit 1
fi

swcli_wheel="$1/dist/starwhale-${PYPI_RELEASE_VERSION:=0.0.0.dev0}-py3-none-any.whl"
echo $swcli_wheel
if [ ! -f "$swcli_wheel" ];then
    python3 -m pip install -r "$1"/requirements-install.txt
    make -C "$1" build-wheel
fi

cp "$swcli_wheel" "$SCRIPT_DIR"/starwhale-0.0.0.dev0-py3-none-any.whl
