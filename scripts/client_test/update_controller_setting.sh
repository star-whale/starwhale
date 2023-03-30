#!/usr/bin/env bash

set -x

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

curl -X 'POST' \
  "$CONTROLLER_URL/api/v1/system/setting" \
  -H 'accept: application/json' \
  -H "$auth_header"\
  -H 'Content-Type: text/plain' \
  --data-binary @- << EOF
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
EOF

) || exit 1

