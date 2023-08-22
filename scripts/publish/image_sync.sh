#!/usr/bin/env bash

set -e

if [[ -n ${DEBUG} ]]; then
    set -x
fi

work_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
lock_file="$work_dir/lock"
function rm_lock() {
    rm "$lock_file"
}
if test -f "$lock_file"; then
    echo "lock exists. exit"
    exit 0
else
  touch "$lock_file"
  trap rm_lock EXIT
fi

export source_registry=${source_registry:="ghcr.io"}
export source_repo_name=${source_repo_name:="star-whale"}
export target_registry=${target_registry:="homepage-bj.intra.starwhale.ai:5000"}
target_repo_name1=star-whale
target_repo_name2=starwhaleai

regctl_file="$work_dir/regctl"
if ! test -f "$regctl_file"; then
    curl -L https://github.com/regclient/regclient/releases/latest/download/regctl-linux-amd64 >$regctl_file
    chmod 755 $regctl_file
    $regctl_file registry set --tls=disabled $target_registry
fi

function copy_image() {
  $regctl_file image copy "$source_registry/$1" "$target_registry/$2"
}

function get_tags() {
  local source_image=$1
  $regctl_file tag -v error ls "$source_registry/$source_repo_name/$source_image"
}

# start to sync images
declare -a starwhale_images=("server" "base" "cuda" "dataset_builder")
for image in "${starwhale_images[@]}"; do
    tags=$(get_tags "$image")
    # if the image has already synced, regctl would skip it.
    for tag in $tags; do
      copy_image "$source_repo_name/$image:$tag" "$target_repo_name1/$image:$tag"
      copy_image "$source_repo_name/$image:$tag" "$target_repo_name2/$image:$tag"
  done
done
