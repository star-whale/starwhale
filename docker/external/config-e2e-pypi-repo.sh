#!/usr/bin/env bash

set -e

if [[ ! -z ${DEBUG} ]]; then
    set -x
fi

if [ "$1" = "1" ] ; then
    echo "overwrite pip config..."
    mkdir /root/.pip
    echo "[global]
index-url = http://$2:$3/repository/pypi-hosted/simple
extra-index-url=https://pypi.org/simple
                https://pypi.doubanio.com/simple/
                https://pypi.tuna.tsinghua.edu.cn/simple/
                http://pypi.mirrors.ustc.edu.cn/simple/


[install]
trusted-host=$2
             pypi.mirrors.ustc.edu.cn
" > /root/.pip/pip.conf

fi
