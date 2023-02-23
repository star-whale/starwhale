#!/usr/bin/env bash
#docker run -v /var/run/docker.sock:/var/run/docker.sock -v ~/.kube:/root/.kube -v ~/code/starwhale:/starwhale --env-file ~/.sw/setup.env -e SWNAME=e2e -e SWNS=e2e -e RESOURCE_POOL=x86 -e YARN_REGISTRY=https://registry.npm.taobao.org -it sw-e2e

set -x

generate_post_data()
{
cat <<EOF
{
"msg_type": "text",
"content": {"text":"$1"}
}
EOF
}
send_feishu() {
  if [[ -n "$FEISHU_HOOK" ]] ; then
    curl -X POST -H "Content-Type: application/json" \
    -d "$(generate_post_data "$1")" \
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

python3 -m pip install --upgrade pip
python3 -m pip config set global.cache-dir /.cache/pip
python3 -m pip config set global.default-timeout 300
cd /starwhale/scripts/e2e_test
if [[ -z "$PUBLISH" ]] ; then
  if bash start_test.sh ;then
    send_feishu "e2e SUCCESS: console path http://$SWNAME.pre.intra.starwhale.ai"
  else
    send_feishu "e2e FAIL: ns:$SWNS error log is: $LOG_STORAGE/log/log.$(date +'%Y%m%d')"
  fi
else
  cd /starwhale/scripts/publish
  bash pub.sh $PUBLISH -s --app $SWNAME --ns $SWNS
fi
