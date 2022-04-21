package ai.starwhale.mlops.agent.container.impl;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import cn.hutool.core.collection.CollectionUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
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
    public Optional<String> startContainer(ImageConfig imageConfig) {
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

            if(CollectionUtil.isNotEmpty(imageConfig.getEntrypoint())) {
                createContainerCmd.withEntrypoint(imageConfig.getEntrypoint());
            }

            if(CollectionUtil.isNotEmpty(imageConfig.getEnv())) {
                createContainerCmd.withEnv(imageConfig.getEnv());
            }
            // exec create cmd
            CreateContainerResponse response = createContainerCmd.exec();

            if (StringUtils.hasText(response.getId())) {
                client.startContainerCmd(response.getId()).exec();
                return Optional.of(response.getId());
            }

        } catch (NotFoundException e) {
            log.error("image:{} not found at local, try to pull from remote", imageConfig.getImage());
            ResultCallback.Adapter<PullResponseItem> resultCallback = client.pullImageCmd(imageConfig.getImage()).start();
            try {
                resultCallback.awaitCompletion();
                // one more again
                this.startContainer(imageConfig);
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

    @Override
    public Optional<ContainerStatus> status(String containerId) {
        return Optional.empty();
    }
}
