#!/usr/bin/env bash
# $1 the release of the test
# $2 the host for the pv path 

set -x 

kubectl delete namespace "$1"
ssh "$2" "sudo rm -rf /mnt/data/starwhale/$1"