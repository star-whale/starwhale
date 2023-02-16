#!/usr/bin/env bash

var=$(lsof -i tcp:8082)
if [ ! -z "$var" ]
then
  sleep 10
else
  if kill -9 `ps -ef|grep port-forward | grep -v grep | awk '{print $2}'` ; then echo "kill success and restart port-forward"; fi
  nohup kubectl port-forward --namespace starwhale-e2e svc/controller 8082:8082 &
  sleep 10
fi

