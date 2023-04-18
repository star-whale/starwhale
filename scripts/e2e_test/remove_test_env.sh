#!/usr/bin/env bash
# $1 the release of the test
# $2 the host for the pv path

set -x

helm uninstall "$1" -n "$1" || echo "helm uninstall ns error"
kubectl delete ns "$1" || echo "kubectl delete ns error"
ssh "$2" "sudo rm -rf /mnt/data/starwhale/$1"
