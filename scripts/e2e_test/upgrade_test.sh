#!/usr/bin/env bash

set -e
if [[ -n ${DEBUG} ]]; then
  set -x
fi

SW_USER="${SW_USER:=starwhale}"
SW_PWD="${SW_PWD:=abcd1234}"
HOST_URL="${HOST_URL:=upgrade-test.pre.intra.starwhale.ai}"
IMAGE="$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION"

HEADER_AUTH=$(curl -s -D - http://$HOST_URL/api/v1/login -d 'userName='${SW_USER}'&userPwd='${SW_PWD} | grep Authorization:)

RESULT=$(curl -s -D - http://$HOST_URL/api/v1/system/version/upgrade -H "Content-Type: application/json" -H "${HEADER_AUTH}" -d '{"version": "ignored", "image": "'"${IMAGE}"'"}')

echo "$RESULT" | grep HTTP

