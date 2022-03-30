package ai.starwhale.mlops.agent.container.impl;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import java.util.Map;
import java.util.Optional;

/**
 * Docker client
 */
public class DockerContainerClient implements ContainerClient {

    private final DockerClient client;

    public DockerContainerClient(DockerClient client) {
        this.client = client;
    }

    @Override
    public Optional<String> startContainer(String imageId, ImageConfig imageConfig) {
        CreateContainerResponse response = client.createContainerCmd(imageId)
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withNetworkMode(imageConfig.getNetworkMode())
                    .withDevices()
                    .withAutoRemove(imageConfig.getAutoRemove()) // usually is false
            )
            .withEnv(imageConfig.getEnv())
            .withLabels(imageConfig.getLabels())
            .exec();
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> stopContainer(String containerId) {
        // todo: stop and clean middle dirty result
        return Optional.empty();
    }

    @Override
    public Optional<ContainerStatus> status(String containerId) {
        return Optional.empty();
    }
}
