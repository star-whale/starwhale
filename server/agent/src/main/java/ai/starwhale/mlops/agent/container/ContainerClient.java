package ai.starwhale.mlops.agent.container;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.Builder;
import lombok.Data;

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
    Optional<String> createAndStartContainer(ImageConfig config);
    boolean stopAndRemoveContainer(String containerId, boolean deleteVolume);
    boolean startContainer(String containerId);
    boolean stopContainer(String containerId);
    boolean removeContainer(String containerId, boolean deleteVolume);
    void logContainer(String containerId, ResultCallback<Frame> resultCallback);
    ContainerInfo containerInfo(String containerId);
    ContainerStatus status(String containerId);

    @Data
    @Builder
    class ContainerInfo{
        String logPath;
    }

    /**
     * "created""running""paused""restarting""removing""exited""dead"
     */
    enum ContainerStatus {
        /**
         * normal life cycle
         */
        NORMAL,
        /**
         * occur some error
         */
        DEAD,

        /**
         * 404 no such container
         */
        NO_SUCH_CONTAINER
    }
}
