#!/usr/bin/env bash

set -e
if [[ -n ${DEBUG} ]]; then
  set -x
fi

SW_USER="${SW_USER:=starwhale}"
SW_PWD="${SW_PWD:=abcd1234}"
HOST_URL="${HOST_URL:=upgrade-test.pre.intra.starwhale.ai}"
new_image="$NEXUS_HOSTNAME:$PORT_NEXUS_DOCKER/star-whale/server:$SERVER_RELEASE_VERSION"

header_auth=$(curl -s -D - http://$HOST_URL/api/v1/login -d 'userName='${SW_USER}'&userPwd='${SW_PWD} | grep Authorization:)
# Send upgrade request to upgrade-test deployment
result=$(curl -s -D - http://$HOST_URL/api/v1/system/version/upgrade -H "Content-Type: application/json" -H "${header_auth}" -d '{"version": "ignored", "image": "'"${new_image}"'"}')
echo "$result" | grep HTTP


# Set the timeout in seconds
timeout=600

# Loop until the deployment is updated with the new image or the timeout is reached
wait_for_image_update() {
    namespace="upgrade-test"
    new_image_tag="$1"
    while true; do
        # Get the current image tag for the deployment
        current_image=$(kubectl get deployment controller -n "${namespace}" -o=jsonpath='{.spec.template.spec.containers[0].image}')

        # Check if the current image tag matches the new image tag
        if [[ "${current_image}" == *"${new_image_tag}"* ]]; then
            echo "Deployment image is updated with tag ${new_image_tag}!"
            exit 0
        fi

        # If the deployment is not updated yet, wait for 10 seconds before checking again
        echo "Waiting for deployment image to be updated with tag ${new_image_tag}..."
        sleep 10
    done
}

# Run the wait_for_image_update function with a timeout
timeout ${timeout} bash -c "$(declare -f wait_for_image_update); wait_for_image_update ${new_image}"

# Check the exit status of the wait_for_image_update function
if [[ $? -eq 124 ]]; then
    echo "Timed out waiting for deployment image to be updated with tag ${new_image_tag}!"
    exit 1
fi

