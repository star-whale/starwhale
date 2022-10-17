#!/usr/bin/env bash

set -e

if [[ ! -z ${DEBUG} ]]; then
    set -x
fi
echo "login"
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

echo "get task"
OUT=`curl -X 'GET' \
  "http://$1/api/v1/project/starwhale/job/1/task?pageNum=1&pageSize=10" \
  -H 'accept: application/json' \
  -H "$auth_header" | jq '.data.list'| jq -r '.[]|.id'`
echo "taskids: $OUT..."
read -a task_ids <<< $OUT
task_ids=($OUT)

SAVEIFS=$IFS   # Save current IFS (Internal Field Separator)
IFS=$'\n'      # Change IFS to newline char
task_ids=($OUT) # split the `names` string into an array by the same name
IFS=$SAVEIFS   # Restore original IFS
echo "get logs..."
for (( i=0; i<${#task_ids[@]}; i++ ))
do
  task_id=${task_ids[$i]}
  log_file=`curl -X 'GET'  "http://$1/api/v1/log/offline/$task_id"   -H 'accept: application/json'   -H "$auth_header" | jq -r '.data[0]'`
  echo $log_file

  echo "task log is:"
  curl -X 'GET' "http://$1/api/v1/log/offline/$task_id/$log_file" -H 'accept: plain/text' -H "$auth_header"
done

echo "get job status"

curl -X 'GET' \
    "http://$1/api/v1/project/starwhale/job/1" \
    -H 'accept: application/json' \
    -H "$auth_header" | jq -r '.data.jobStatus' > jobStatus
job_status=`cat jobStatus`

echo "job status is $job_status"
if [[ "$job_status" != "SUCCESS" ]] ; then
  exit 1
fi
