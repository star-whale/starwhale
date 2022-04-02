package ai.starwhale.mlops.agent.container;

import java.util.Optional;

/**
 * the upper layer of the concrete implementation.
 */
public interface ContainerClient {
    /**
     *
     * @param config start param
     * @return container id
     */
    Optional<String> startContainer(ImageConfig config);
    boolean stopAndRemoveContainer(String containerId, boolean deleteVolume);
    boolean stopContainer(String containerId);
    boolean removeContainer(String containerId, boolean deleteVolume);
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
