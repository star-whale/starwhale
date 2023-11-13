#!/usr/bin/env bash

set -e

INSTANCES=("local")
[ "$CONTROLLER_URL" ] && INSTANCES=("local" "server")

for ins in "${INSTANCES[@]}"; do
  swcli instance select "$ins"
  for rc in "model" "runtime" "dataset"; do
      swcli $rc ls
      swcli -o json $rc ls
  done
done
