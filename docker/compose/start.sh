#!/usr/bin/env bash

--help() {
  echo "Usage: start.sh [OPTIONS]"
  echo "Options:"
  echo "  --help        Show this message and exit."
  echo "  --global-ip   use this ip as the access point of server. "
  echo "                You don't need to set this option if you have set it before."
  echo "                If you have a firewall configured, you'd better to ACCEPT the INPUT from the global-ip as dst ."
}

--global-ip(){
  if test -z "$1"; then
    echo "value is needed by this option"
    exit 1
  else
    echo "GLOBAL_IP=$1" | tee .env
    startup
  fi
}

startup() {
  if test -f ".env" ; then
    docker compose up
  else
    echo "please use --global-ip option when you run the script for the first time"
  fi
}

if test -z "$1"; then
  startup
else
  $1 "$2"
fi