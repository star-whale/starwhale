package ai.starwhale.mlops.agent.container.impl;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import cn.hutool.core.collection.CollectionUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.api.model.HostConfig;

import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

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
        CreateContainerResponse response = client.createContainerCmd(imageConfig.getImage())
            .withHostConfig(hostConfig)
            .withEnv(imageConfig.getEnv())
            .withLabels(imageConfig.getLabels())
            .withEntrypoint(imageConfig.getEntrypoint())
            .exec();
        if (StringUtils.hasText(response.getId())) {
            return Optional.of(response.getId());
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
