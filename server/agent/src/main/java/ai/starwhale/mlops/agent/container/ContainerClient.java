/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
