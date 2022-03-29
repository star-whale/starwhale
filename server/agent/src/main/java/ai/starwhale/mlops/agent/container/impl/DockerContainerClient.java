package ai.starwhale.mlops.agent.container.impl;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import com.github.dockerjava.api.DockerClient;
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
