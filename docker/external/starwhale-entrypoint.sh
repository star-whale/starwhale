#!/usr/bin/env bash

set -e

if [ "${SW_TASK_DISABLE_DEBUG}" != "1" ]; then
    set -x
fi

ulimit -n 65535 || true

CONDA_BIN="/opt/miniconda3/bin"
PIP_CACHE_DIR=${SW_PIP_CACHE_DIR:=/"${SW_USER:-root}"/.cache/pip}
PYTHON_VERSION=${SW_RUNTIME_PYTHON_VERSION:-"3.8"}

set_python_alter() {
    echo "-->[Preparing] set python/python3 to $PYTHON_VERSION ..."
    update-alternatives --install /usr/bin/python3 python3 /usr/bin/python"$PYTHON_VERSION" 10
    update-alternatives --install /usr/bin/python python /usr/bin/python"$PYTHON_VERSION" 10
    python3 --version
}

set_pip_config() {
    echo "-->[Preparing] config pypi and conda config ..."

    if [ ${SW_PYPI_INDEX_URL} ] ; then
        echo -e "\t ** use SW_PYPI_* env to config ~/.pip/pip.conf"
        mkdir -p ~/.pip
        cat > ~/.pip/pip.conf << EOF
[global]
index-url = ${SW_PYPI_INDEX_URL}
extra-index-url = ${SW_PYPI_EXTRA_INDEX_URL}
timeout = ${SW_PYPI_TIMEOUT:-90}

[install]
trusted-host= ${SW_PYPI_TRUSTED_HOST}
EOF
        echo -e "\t ** current pip conf:"
        echo "-------------------"
        cat ~/.pip/pip.conf
        echo "-------------------"
    else
        echo -e "\t ** use image builtin pip.conf"
    fi
}

set_pip_cache() {
    echo "\t ** set pip cache dir:"
    python3 -m pip config set global.cache-dir ${PIP_CACHE_DIR} || true
    python3 -m pip cache dir || true
}

set_py_and_sw() {
    echo "**** DETECT RUNTIME: python version: ${PYTHON_VERSION}, starwhale version: ${SW_VERSION}"

    echo "-->[Preparing] Set pip config."
    set_pip_config

    echo "-->[Preparing] Use python:${PYTHON_VERSION}."
    set_python_alter

    set_pip_cache
    echo "-->[Preparing] Install starwhale:${SW_VERSION}."
    # install starwhale for current python
    python3 -m pip install "starwhale==${SW_VERSION}" || exit 1
    rm -rf /usr/local/bin/swcli /usr/local/bin/sw-docker-entrypoint
    ln -s /opt/starwhale.venv/bin/swcli /usr/local/bin/swcli
    ln -s /opt/starwhale.venv/bin/sw-docker-entrypoint /usr/local/bin/sw-docker-entrypoint
}

set_py_and_sw
sw-docker-entrypoint "$1"
