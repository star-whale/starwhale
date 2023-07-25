#!/usr/bin/env bash

set -x

in_github_action() {
  [ -n "$GITHUB_ACTION" ]
}

[ "$CONTROLLER_URL" ] && (
resps=$(curl -D - --location --request POST "$CONTROLLER_URL/api/v1/login" \
    --header 'Accept: application/json' \
    --form 'userName="starwhale"' \
    --form 'userPwd="abcd1234"' -o /dev/null)
IFS='
'
set -f
for line in $resps; do
  if [[ "${line}" =~ ^Authorization.* ]] ; then
    export auth_header="${line}"
    break
  fi
done

export auth_header=$(echo ${auth_header%$'\r'})

if ! in_github_action; then
  data=$(cat << EOF
---
storageSetting:
- type: "minio"
  tokens:
    bucket: "users"
    ak: "starwhale"
    sk: "starwhale"
    endpoint: "http://10.131.0.1:9000"
    region: "local"
    hugeFileThreshold: "10485760"
    hugeFilePartSize: "5242880"
- type: "s3"
  tokens:
    bucket: "users"
    ak: "starwhale"
    sk: "starwhale"
    endpoint: "http://10.131.0.1:9000"
    region: "local"
    hugeFileThreshold: "10485760"
    hugeFilePartSize: "5242880"
resourcePoolSetting:
- name: "default"
  resources:
    - name: "cpu"
      defaults: 5.0
    - name: "memory"
      defaults: 3145728
    - name: "nvidia.com/gpu"
pypiSetting:
  indexUrl: "http://$NEXUS_HOSTNAME:$PORT_NEXUS/repository/$REPO_NAME_PYPI/simple"
  extraIndexUrl: "$SW_PYPI_EXTRA_INDEX_URL"
  trustedHost: "$NEXUS_HOSTNAME"
  retries: 10
  timeout: 90
datasetBuild:
  resourcePool: "default"
  image: "docker-registry.starwhale.cn/star-whale/base:latest"
  clientVersion: "$PYPI_RELEASE_VERSION"
  pythonVersion: "3.10"
EOF
  )
  curl -X 'POST' \
  "$CONTROLLER_URL/api/v1/system/setting" \
  -H 'accept: application/json' \
  -H "$auth_header"\
  -H 'Content-Type: text/plain' \
  --data-binary "${data}"
fi

) || exit 1

