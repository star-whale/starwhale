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

curl -X 'POST' \
  "$CONTROLLER_URL/api/v1/system/setting" \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxLHN0YXJ3aGFsZSIsImlzcyI6InN0YXJ3aGFsZSIsImlhdCI6MTY2OTMzMjIyOSwiZXhwIjoxNjcxOTI0MjI5fQ.hmv-reV5kuLwt_KR_xWZSEYLDMCCaHDSjD0N78HyBFJa53w0W-Q91j8LgCgMwp9AWGt5fU9lVPvATD69xDiBdg'\
  -H 'Content-Type: text/plain' \
  -d $'---\nstorageSetting:\n- type: "minio"\n  tokens:\n    bucket: "users"\n    ak: "starwhale"\n    sk: "starwhale"\n    endpoint: "http://10.131.0.1:9000"\n    region: "local"\n    hugeFileThreshold: "10485760"\n    hugeFilePartSize: "5242880"\n- type: "s3"\n  tokens:\n    bucket: "users"\n    ak: "starwhale"\n    sk: "starwhale"\n    endpoint: "http://10.131.0.1:9000"\n    region: "local"\n    hugeFileThreshold: "10485760"\n    hugeFilePartSize: "5242880"\n'
)

