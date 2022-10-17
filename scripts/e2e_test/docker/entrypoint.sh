#!/usr/bin/env bash
#docker run -v /var/run/docker.sock:/var/run/docker.sock -v ~/.kube:/root/.kube -v ~/code/starwhale:/starwhale --env-file ~/.sw/setup.env -e SWNAME=e2e -e SWNS=e2e -e RESOURCE_POOL=x86 -e YARN_REGISTRY=https://registry.npm.taobao.org -it sw-e2e

set -x

send_feishu() {
    if [[ -n "$FEISHU_HOOK" ]] ; then
        curl -X POST -H "Content-Type: application/json" \
        -d '{"msg_type":"text","content":{"text":"$1"}}' \
        ${FEISHU_HOOK}
    fi
}

export SWREPO="${SWREPO:=https://github.com/star-whale/starwhale.git}"

if [[ -n "$YARN_REGISTRY" ]] ; then
  npm config set registry "${YARN_REGISTRY}"
  yarn config set registry "${YARN_REGISTRY}"
fi
if ! test -d /starwhale; then
  git lfs clone "$SWREPO"
fi
git config --global --add safe.directory /starwhale
git config --global user.email "renyanda@starwhale.ai"
cd /starwhale/scripts/e2e_test
if [[ -z "$PUBLISH" ]] ; then
  if source start_test.sh ;then
    send_feishu "e2e SUCCESS ns:$SWNS name:$SWNAME"
  else
    send_feishu "e2e FAIL ns:$SWNS name:$SWNAME"
  fi
else
  cd /starwhale/scripts/publish
  bash pub.sh $PUBLISH -s --app $SWNAME --ns $SWNS
fi
