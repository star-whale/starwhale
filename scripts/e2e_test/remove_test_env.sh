#!/usr/bin/env bash
# $1 the release of the test
# $2 the host for the pv path

set -x
if [ -f "$HOME"/.config/e2e.config ];then
  white_list_array=($(grep white_list "$HOME"/.config/e2e.config | cut -d "=" -f2 | tr ',' ' '))

  for ns in "${white_list_array[@]}";do
    if [ "$ns" == "$1" ];then
      echo "$1 is in white list, skip cleanup"
      exit 0
    fi
  done
else
  echo "$HOME/.config/e2e.config does not exist, skip white list check."
fi

helm uninstall "$1" -n "$1" || echo "helm uninstall ns error"
kubectl delete ns "$1" || echo "kubectl delete ns error"
ssh "$2" "sudo rm -rf /mnt/data/starwhale/$1"
