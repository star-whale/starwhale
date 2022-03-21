package ai.starwhale.mlops.agent.container;

import java.util.Optional;

/**
 * the upper layer of the concrete implementation.
 */
public interface ContainerClient {
    /**
     *
     * @param imageId
     * @param args
     * @return container id
     */
    Optional<String> startContainer(String imageId, String[] args);
    Optional<Boolean> stopContainer(String containerId);
    Optional<ContainerStatus> status(String containerId);

    enum ContainerStatus {

        /**
         * running
         */
        RUNNING,

        /**
         * 404 no such container
         */
        NO_SUCH_CONTAINER,

        /**
         * server error
         */
        SERVER_ERROR
    }
}
