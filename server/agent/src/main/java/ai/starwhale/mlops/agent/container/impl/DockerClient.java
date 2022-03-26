package ai.starwhale.mlops.agent.container.impl;

import ai.starwhale.mlops.agent.container.ContainerClient;

import ai.starwhale.mlops.agent.container.ImageConfig;
import com.github.dockerjava.transport.DockerHttpClient;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Docker client
 */
@Service
public class DockerClient implements ContainerClient {

    private final DockerHttpClient client;

    public DockerClient(DockerHttpClient client) {
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
