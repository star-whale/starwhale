#!/usr/bin/env bash
# $1 the release of the test
# $2 the host for the pv path 

set -x 

helm uninstall "$1" -n "$1"
kubectl delete pvc data-"$1"-mysql-0 -n "$1"
ssh "$2" "sudo rm -rf /mnt/data/starwhale/$1"