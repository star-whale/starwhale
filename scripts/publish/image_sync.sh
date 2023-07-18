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

if [[ -z ${GH_TOKEN} ]]; then
  echo "GH_TOKEN not set"
  exit 1
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

# start to sync sw-version images
declare -i page=1
while true
do
  release=$(curl \
              -H "Accept: application/vnd.github+json" \
              -H "Authorization: Bearer ${GH_TOKEN}" \
              "https://api.github.com/repos/star-whale/starwhale/releases?per_page=1&page=$page")
  if [ $(echo "$release" | jq -r  '.[0].draft')  == "true" ] ;
    then echo "draft release $(echo "$release" | jq -r  '.[0].name')";
    page+=1
  else
    export release_version=$(echo "$release" | jq -r '.[0].tag_name')
    #trip v
    release_version=${release_version:1}
    echo "real release found $release_version";
    break
  fi
done
last_version_file="$work_dir/last_version"

if last_version=$(cat "$last_version_file") ; then echo "last_version is $last_version"; fi
if [ "$last_version"  == "$release_version" ] ; then
  echo "release already synced"
else
  copy_image "$source_repo_name/server:$release_version" "$target_repo_name1/server:$release_version"
  copy_image "$source_repo_name/server:$release_version" "$target_repo_name2/server:$release_version"
  copy_image "$source_repo_name/server:$release_version" "$target_repo_name1/server:latest"
  copy_image "$source_repo_name/server:$release_version" "$target_repo_name2/server:latest"
  echo "$release_version" > "$last_version_file"
fi

# start to sync base images
release_version=${FIXED_VERSION_BASE_IMAGE:-"0.3.0"}

last_version_file="$work_dir/last_version_for_base"

if last_version=$(cat "$last_version_file") ; then echo "last_version_for_base is $last_version"; fi
if [ "$last_version"  == "$release_version" ] ; then
  echo "release already synced"
else
  # base image
  copy_image "$source_repo_name/base:$release_version" "$target_repo_name1/base:$release_version"
  copy_image "$source_repo_name/base:$release_version" "$target_repo_name2/base:$release_version"
  copy_image "$source_repo_name/base:$release_version" "$target_repo_name1/base:latest"
  copy_image "$source_repo_name/base:$release_version" "$target_repo_name2/base:latest"

  # cuda image
  declare -a starwhale_image_prefix=("" "11.3" "11.3-cudnn8" "11.4" "11.4-cudnn8" "11.5" "11.5-cudnn8" "11.6" "11.6-cudnn8" "11.7")
  for pre in "${starwhale_image_prefix[@]}"
    do
      # starwhaleai/cuda:11.5-cudnn8-base0.2.7
      # starwhaleai/cuda:11.7-base0.2.5
      copy_image "$source_repo_name/cuda:$pre-base$release_version" "$target_repo_name1/cuda:$pre-base$release_version"
      copy_image "$source_repo_name/cuda:$pre-base$release_version" "$target_repo_name2/cuda:$pre-base$release_version"
    done
fi
