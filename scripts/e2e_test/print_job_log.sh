#!/usr/bin/env bash

set -e

if [[ ! -z ${DEBUG} ]]; then
    set -x
fi

curl -D - --location --request POST "http://$1/api/v1/login" \
--header 'Accept: application/json' \
--form 'userName="starwhale"' \
--form 'userPwd="abcd1234"' -o /dev/null | while read line
do
  if [[ "${line}" =~ ^Authorization.* ]] ; then
    echo "${line}" > auth_header.h
  fi
done

auth_header=`cat auth_header.h`
sudo apt-get install jq

OUT=`curl -X 'GET' \
  "http://$1/api/v1/project/1/job/1/task?pageNum=1&pageSize=10" \
  -H 'accept: application/json' \
  -H "$auth_header" | | jq '.data.list'| jq -r '.[]|.id'

task_ids=$(echo $OUT | tr "\n")
echo $task_ids

for taskid in $task_ids
do
  curl -X 'GET'  "http://$1/api/v1/log/offline/$task_id"   -H 'accept: application/json'   -H "$auth_header" | jq -r '.data[0]'
  log_file=`curl -X 'GET'  "http://$1/api/v1/log/offline/$task_id"   -H 'accept: application/json'   -H "$auth_header" | jq -r '.data[0]'`
  echo $log_file

  echo "task log is:"
  curl -X 'GET' "http://$1/api/v1/log/offline/$task_id/$log_file" -H 'accept: plain/text' -H "$auth_header"
done
