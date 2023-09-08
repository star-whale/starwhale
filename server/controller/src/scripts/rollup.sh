#!/bin/bash

#
# Copyright 2022 Starwhale, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# This script is used to roll up the controller server

set -x
CONTROLLER_SERVER_ADDRESS_NEW="http://localhost:8082"
CONTROLLER_SERVER_ADDRESS_OLD="http://localhost:8083"
echo "Please Confirm the server address:\n the new Instance is $CONTROLLER_SERVER_ADDRESS_NEW, \n the old Instance is: $CONTROLLER_SERVER_ADDRESS_OLD"
read -p "Please confirm the server address y/n: " CONFIRM_SERVER_ADDRESS
# equals ignore case
CONFIRM_SERVER_ADDRESS=$(echo "$CONFIRM_SERVER_ADDRESS" | tr '[:upper:]' '[:lower:]')
if [ "$CONFIRM_SERVER_ADDRESS" != "y" ]; then
  echo "Please confirm the server address"
  exit 1
fi
echo "Please copy the token from the console of the new server"
read -p "token copied from console: " AUTH_TOKE

status_notify() {
  INSTANCE_TYPE="NEW"
  if [ "$1" = "$CONTROLLER_SERVER_ADDRESS_NEW" ]; then
    INSTANCE_TYPE="OLD"
  fi
  response_code=$(curl -X GET "$1/api/v1/system/upgrade/instance/status" \
                       -H "Authorization: $AUTH_TOKE" \
                       -F "status=$2&instanceType=$INSTANCE_TYPE" | jq -r '.data.code')
  if [ "$response_code" = "Success" ]; then
    echo "status_notify success"
  else
    echo "status_notify failed, please check it manually"
    exit 1
  fi
}

# notify the old server that the new server is ready
new_ready_up() {
  status_notify "$CONTROLLER_SERVER_ADDRESS_OLD" "READY_UP"
}
# notify the new server that the old server is ready
old_ready_down() {
  status_notify "$CONTROLLER_SERVER_ADDRESS_NEW" "READY_DOWN"
}
# notify the old server that the new server is up
new_up() {
  status_notify "$CONTROLLER_SERVER_ADDRESS_OLD" "UP"
}
# notify the new server that the old server is down
old_down() {
  status_notify "$CONTROLLER_SERVER_ADDRESS_NEW" "DOWN"
}

# start the new server automatically
if -z "$1"; then
  new_ready_up && old_ready_down && new_up && old_down
else
# start the new server manually
  $1
fi
