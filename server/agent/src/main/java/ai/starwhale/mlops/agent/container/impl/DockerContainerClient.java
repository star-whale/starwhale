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

package ai.starwhale.mlops.agent.container.impl;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import cn.hutool.core.collection.CollectionUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Docker client
 */
@Slf4j
public class DockerContainerClient implements ContainerClient {

    private final DockerClient client;

    public DockerContainerClient(DockerClient client) {
        this.client = client;
    }

    @Override
    public Optional<String> createAndStartContainer(ImageConfig imageConfig) {
        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withNetworkMode(imageConfig.getNetworkMode())
                    .withAutoRemove(imageConfig.getAutoRemove()); // usually is false
            // gpu config
            if (imageConfig.getGpuConfig() != null) {
                DeviceRequest deviceRequest = new DeviceRequest();
                deviceRequest.withCapabilities(imageConfig.getGpuConfig().getCapabilities());
                deviceRequest.withDeviceIds(imageConfig.getGpuConfig().getDeviceIds());
                deviceRequest.withCount(imageConfig.getGpuConfig().getDeviceIds().size());
                hostConfig.withDeviceRequests(List.of(deviceRequest));
            }

            // cpu config
            if (imageConfig.getCpuConfig() != null) {
                hostConfig.withCpuCount(imageConfig.getCpuConfig().getCpuCount());
                hostConfig.withCpuPeriod(imageConfig.getCpuConfig().getCpuPeriod());
                hostConfig.withCpuQuota(imageConfig.getCpuConfig().getCpuQuota());
                hostConfig.withCpuPercent(imageConfig.getCpuConfig().getCpuPercent());
            }

            // io config
            if (imageConfig.getIoConfig() != null) {
                hostConfig.withIoMaximumBandwidth(imageConfig.getIoConfig().getIoMaximumBandwidth());
                hostConfig.withIoMaximumIOps(imageConfig.getIoConfig().getIoMaximumIOps());
            }

            // mount config
            if (CollectionUtil.isNotEmpty(imageConfig.getMounts())) {
                hostConfig.withMounts(
                        imageConfig.getMounts().stream()
                                .map(mount -> {
                                    Mount m = new Mount();
                                    m.withReadOnly(mount.getReadOnly());
                                    m.withSource(mount.getSource());
                                    m.withTarget(mount.getTarget());
                                    m.withType(MountType.valueOf(mount.getType()));
                                    return m;
                                })
                                .collect(Collectors.toList()));
            }

            CreateContainerCmd createContainerCmd = client.createContainerCmd(imageConfig.getImage())
                    .withHostConfig(hostConfig)
                    .withLabels(imageConfig.getLabels())
                    .withCmd(imageConfig.getCmd());

            if (CollectionUtil.isNotEmpty(imageConfig.getEntrypoint())) {
                createContainerCmd.withEntrypoint(imageConfig.getEntrypoint());
            }

            if (CollectionUtil.isNotEmpty(imageConfig.getEnv())) {
                createContainerCmd.withEnv(imageConfig.getEnv());
            }
            // exec create cmd
            CreateContainerResponse response = createContainerCmd.exec();

            if (StringUtils.hasText(response.getId())) {
                this.startContainer(response.getId());
                return Optional.of(response.getId());
            }

        } catch (NotFoundException e) {
            log.error("image:{} not found at local, try to pull from remote", imageConfig.getImage());
            ResultCallback.Adapter<PullResponseItem> resultCallback = client.pullImageCmd(imageConfig.getImage()).start();
            try {
                resultCallback.awaitCompletion();
                // one more again
                this.createAndStartContainer(imageConfig);
            } catch (InterruptedException ex) {
                log.error("unknown error:{}", ex.getMessage(), ex);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean stopAndRemoveContainer(String containerId, boolean deleteVolume) {

        if (this.stopContainer(containerId)) {
            return this.removeContainer(containerId, deleteVolume);
        }

        return false;
    }

    @Override
    public boolean startContainer(String containerId) {
        client.startContainerCmd(containerId).exec();
        return true;
    }

    @Override
    public boolean stopContainer(String containerId) {
        try {
            client.stopContainerCmd(containerId).withTimeout(1000).exec();
            return true;
        } catch (Exception e) {
            log.error("stopContainer error:{}", e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean removeContainer(String containerId, boolean deleteVolume) {
        try {
            client.removeContainerCmd(containerId).withRemoveVolumes(deleteVolume).exec();
            return true;
        } catch (Exception e) {
            log.error("removeContainer error:{}", e.getMessage(), e);
        }

        return false;
    }

    public void logContainer(String containerId, ResultCallback<Frame> resultCallback) {
        client.logContainerCmd(containerId)
                .withTailAll()
                .withFollowStream(true).withStdOut(true).withStdErr(true)
                .exec(resultCallback);
    }

    @Override
    public ContainerInfo containerInfo(String containerId) {
        InspectContainerResponse response = client.inspectContainerCmd(containerId).exec();
        return ContainerInfo.builder().logPath(response.getLogPath()).build();
    }

    @Override
    public ContainerStatus status(String containerId) {
        try {
            InspectContainerResponse response = client.inspectContainerCmd(containerId).exec();
            if (Boolean.TRUE.equals(response.getState().getDead()) ||
                    Boolean.TRUE.equals(response.getState().getOOMKilled()) ||
                    Boolean.FALSE.equals(response.getState().getRunning())) {
                return ContainerStatus.DEAD;
            } else {
                return ContainerStatus.NORMAL;
            }
        } catch (NotFoundException e) {
            log.error("container:{} not found ", containerId, e);
            return ContainerStatus.NO_SUCH_CONTAINER;
        }
    }
}
