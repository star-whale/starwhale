package ai.starwhale.mlops.agent.container;

import java.util.Optional;

/**
 * the upper layer of the concrete implementation.
 */
public interface ContainerClient {
    /**
     *
     * @param imageId
     * @param config
     * @return container id
     */
    Optional<String> startContainer(String imageId, ImageConfig config);
    Optional<Boolean> stopContainer(String containerId);
    Optional<ContainerStatus> status(String containerId);

    /**
     * "created""running""paused""restarting""removing""exited""dead"
     */
    enum ContainerStatus {
        /**
         * normal life cycle
         */
        CREATED, RUNNING, PAUSED, RESTARTING, REMOVING, EXITED, DEAD,

        /**
         * 404 no such container
         */
        NO_SUCH_CONTAINER
    }
}
