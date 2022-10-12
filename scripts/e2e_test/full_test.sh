#docker run -v /var/run/docker.sock:/var/run/docker.sock -v ~/.kube:/root/.kube -v ~/starwhale_code:/starwhale --env-file ~/.sw/setup.env -e SWNAME=e2e -e SWNS=e2e -e YARN_REGISTRY=https://registry.npm.taobao.org -it  homepage-ca.intra.starwhale.ai:5000/docker-e2e:0.6 bash
export SWREPO="${SWREPO:=https://github.com/star-whale/starwhale.git}"

if [[ -n "$YARN_REGISTRY" ]] ; then
  npm config set registry "${YARN_REGISTRY}"
  yarn config set registry "${YARN_REGISTRY}"
fi
if ! test -d /starwhale; then
  git lfs clone "$SWREPO"
fi
cd /starwhale/scripts/e2e_test
if [[ -z "$PUBLISH" ]] ; then
  bash start_test.sh
else
  cd /starwhale/scripts/publish
  bash pub.sh $PUBLISH -s --app $SWNAME --ns $SWNS
fi
