#!/usr/bin/env bash

set -e

if [[ -n ${DEBUG} ]]; then
    set -x
fi

if [[ -z ${GH_TOKEN} ]]; then
  echo "GH_TOKEN not set"
  exit 1
fi

#export remote_registry="ghcr.io"
export remote_registry=${remote_registry:="docker.io"}
#export source_repo_name="star-whale"
export source_repo_name=${source_repo_name:="starwhaleai"}
#export local_registry="homepage-ca.intra.starwhale.ai:5000"
export local_registry=${local_registry:="homepage-bj.intra.starwhale.ai:5000"}
local_repo_name1=star-whale
local_repo_name2=starwhaleai

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
    echo "real release $release_version";
    break
  fi
done
if last_version=$(cat last_version) ; then last_version=""; fi
if [ "$last_version"  == "$release_version" ] ; then
  echo "release already synced"
else
  sudo docker pull "$remote_registry"/"$source_repo_name"/server:"$release_version"
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.3
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.3-cudnn8
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.4
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.4-cudnn8
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.5
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.5-cudnn8
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.6
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.6-cudnn8
  sudo docker pull "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.7

  sudo docker tag "$remote_registry"/"$source_repo_name"/server:"$release_version" "$local_registry"/"$local_repo_name1"/server:"$release_version"
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version" "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.3 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.3
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.3-cudnn8 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.3-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.4 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.4
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.4-cudnn8 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.4-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.5 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.5
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.5-cudnn8 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.5-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.6 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.6
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.6-cudnn8 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.6-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.7 "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.7

  sudo docker tag "$remote_registry"/"$source_repo_name"/server:"$release_version" "$local_registry"/"$local_repo_name2"/server:"$release_version"
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version" "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.3 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.3
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.3-cudnn8 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.3-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.4 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.4
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.4-cudnn8 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.4-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.5 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.5
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.5-cudnn8 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.5-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.6 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.6
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.6-cudnn8 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.6-cudnn8
  sudo docker tag "$remote_registry"/"$source_repo_name"/starwhale:"$release_version"-cuda11.7 "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.7

  sudo docker push "$local_registry"/"$local_repo_name1"/server:"$release_version"
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.3
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.3-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.4
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.4-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.5
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.5-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.6
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.6-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name1"/starwhale:"$release_version"-cuda11.7

  sudo docker push "$local_registry"/"$local_repo_name2"/server:"$release_version"
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.3
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.3-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.4
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.4-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.5
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.5-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.6
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.6-cudnn8
  sudo docker push "$local_registry"/"$local_repo_name2"/starwhale:"$release_version"-cuda11.7

  echo "$release_version" > last_version

fi
