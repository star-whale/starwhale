#!/usr/bin/env bash

set -eu

echo "===================================="
echo "StarWhale @docker in docker"
echo "Date: `date -u +%Y-%m-%dT%H:%M:%SZ`"
echo "===================================="

exec docker-init -- dockerd \
    --config-file=/etc/docker/daemon.json \
    "$@"