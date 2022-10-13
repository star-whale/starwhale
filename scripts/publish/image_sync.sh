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


export source_registry=${source_registry:="docker.io"}
export source_repo_name=${source_repo_name:="starwhaleai"}
export target_registry=${target_registry:="homepage-bj.intra.starwhale.ai:5000"}
target_repo_name1=star-whale
target_repo_name2=starwhaleai

declare -i page=1
declare -a starwhale_image_suffix=("" "-cuda11.3" "-cuda11.3-cudnn8" "-cuda11.4" "-cuda11.4-cudnn8" "-cuda11.5-cudnn8" "-cuda11.6" "-cuda11.6-cudnn8" "-cuda11.7")

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
  sudo docker pull "$source_registry"/"$source_repo_name"/server:"$release_version"
  sudo docker tag "$source_registry"/"$source_repo_name"/server:"$release_version" "$target_registry"/"$target_repo_name1"/server:"$release_version"
  sudo docker tag "$source_registry"/"$source_repo_name"/server:"$release_version" "$target_registry"/"$target_repo_name2"/server:"$release_version"
  sudo docker push "$target_registry"/"$target_repo_name1"/server:"$release_version"
  sudo docker push "$target_registry"/"$target_repo_name2"/server:"$release_version"
  sudo docker image rm "$source_registry"/"$source_repo_name"/server:"$release_version""$suf"
  sudo docker image rm "$target_registry"/"$target_repo_name1"/server:"$release_version""$suf"
  sudo docker image rm "$target_registry"/"$target_repo_name2"/server:"$release_version""$suf"

  for suf in "${starwhale_image_suffix[@]}"
    do
      sudo docker pull "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf"
      sudo docker tag "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf" "$target_registry"/"$target_repo_name1"/starwhale:"$release_version""$suf"
      sudo docker tag "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf" "$target_registry"/"$target_repo_name2"/starwhale:"$release_version""$suf"
      sudo docker push "$target_registry"/"$target_repo_name1"/starwhale:"$release_version""$suf"
      sudo docker push "$target_registry"/"$target_repo_name2"/starwhale:"$release_version""$suf"
      sudo docker image rm "$target_registry"/"$target_repo_name1"/starwhale:"$release_version""$suf"
      sudo docker image rm "$target_registry"/"$target_repo_name2"/starwhale:"$release_version""$suf"
      sudo docker image rm "$source_registry"/"$source_repo_name"/starwhale:"$release_version""$suf"
    done

  echo "$release_version" > "$last_version_file"

fi
