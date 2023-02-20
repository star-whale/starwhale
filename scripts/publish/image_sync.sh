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

declare -i page=1
declare -a starwhale_image_suffix=("" "-cuda11.3" "-cuda11.3-cudnn8" "-cuda11.4" "-cuda11.4-cudnn8" "-cuda11.5" "-cuda11.5-cudnn8" "-cuda11.6" "-cuda11.6-cudnn8" "-cuda11.7")

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
function remove_image() {
    if [[ -n "$last_version" ]]; then
        sudo docker image rm $1
    fi
}
if last_version=$(cat "$last_version_file") ; then echo "last_version is $last_version"; fi
if [ "$last_version"  == "$release_version" ] ; then
  echo "release already synced"
else
  sudo docker pull "$source_registry"/"$source_repo_name"/server:"$release_version"
  sudo docker tag "$source_registry"/"$source_repo_name"/server:"$release_version" "$target_registry"/"$target_repo_name1"/server:"$release_version"
  sudo docker tag "$source_registry"/"$source_repo_name"/server:"$release_version" "$target_registry"/"$target_repo_name2"/server:"$release_version"
  sudo docker tag "$source_registry"/"$source_repo_name"/server:"$release_version" "$target_registry"/"$target_repo_name1"/server:latest
  sudo docker tag "$source_registry"/"$source_repo_name"/server:"$release_version" "$target_registry"/"$target_repo_name2"/server:latest
  sudo docker push "$target_registry"/"$target_repo_name1"/server:"$release_version"
  sudo docker push "$target_registry"/"$target_repo_name2"/server:"$release_version"
  sudo docker push "$target_registry"/"$target_repo_name1"/server:latest
  sudo docker push "$target_registry"/"$target_repo_name2"/server:latest
  remove_image "$source_registry"/"$source_repo_name"/server:"$last_version"
  remove_image "$target_registry"/"$target_repo_name1"/server:"$last_version"
  remove_image "$target_registry"/"$target_repo_name2"/server:"$last_version"

  for suf in "${starwhale_image_suffix[@]}"
    do
      sudo docker pull "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf"
      sudo docker tag "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf" "$target_registry"/"$target_repo_name1"/starwhale:"$release_version""$suf"
      sudo docker tag "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf" "$target_registry"/"$target_repo_name2"/starwhale:"$release_version""$suf"
      sudo docker tag "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf" "$target_registry"/"$target_repo_name1"/starwhale:latest"$suf"
      sudo docker tag "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf" "$target_registry"/"$target_repo_name2"/starwhale:latest"$suf"
      sudo docker push "$target_registry"/"$target_repo_name1"/starwhale:"$release_version""$suf"
      sudo docker push "$target_registry"/"$target_repo_name2"/starwhale:"$release_version""$suf"
      sudo docker push "$target_registry"/"$target_repo_name1"/starwhale:latest"$suf"
      sudo docker push "$target_registry"/"$target_repo_name2"/starwhale:latest"$suf"
      remove_image "$target_registry"/"$target_repo_name1"/starwhale:"$last_version""$suf"
      remove_image "$target_registry"/"$target_repo_name2"/starwhale:"$last_version""$suf"
      remove_image "$source_registry"/"$source_repo_name"/starwhale:"$last_version""$suf"
    done

  echo "$release_version" > "$last_version_file"

fi
