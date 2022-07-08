#!/usr/bin/env bash

set -e

if [[ ! -z ${DEBUG} ]]; then
    set -x
fi

if [ -z $1 ] ; then
    echo "PYPI overwrite is false"
else
    echo "[global]
index-url = http://$2:$3/repository/pypi-hosted/simple
extra-index-url=https://pypi.org/simple

[install]
trusted-host=$2
" > /root/.pip/pip.conf

fi