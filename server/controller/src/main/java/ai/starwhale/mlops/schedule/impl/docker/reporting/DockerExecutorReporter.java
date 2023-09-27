/*
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

package ai.starwhale.mlops.schedule.impl.docker.reporting;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.schedule.impl.docker.ContainerRunMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.impl.docker.RunExecutorDockerImpl;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DockerExecutorReporter {

    final RunReportReceiver runReportReceiver;

    final SystemSettingService systemSettingService;

    final DockerClientFinder dockerClientFinder;

    final ContainerRunMapper containerRunMapper;

    final ContainerStatusExplainer containerStatusExplainer;

    public DockerExecutorReporter(
            RunReportReceiver runReportReceiver,
            SystemSettingService systemSettingService,
            DockerClientFinder dockerClientFinder,
            ContainerRunMapper containerRunMapper,
            ContainerStatusExplainer containerStatusExplainer
    ) {
        this.runReportReceiver = runReportReceiver;
        this.systemSettingService = systemSettingService;
        this.dockerClientFinder = dockerClientFinder;
        this.containerRunMapper = containerRunMapper;
        this.containerStatusExplainer = containerStatusExplainer;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 3000)
    public void reportRuns() {

        List<ResourcePool> resourcePools = systemSettingService.getAllResourcePools();
        if (CollectionUtils.isEmpty(resourcePools)) {
            resourcePools = List.of(new ResourcePool());
        }
        Set<DockerClient> distinctDockerClients = resourcePools.stream()
                .map(resourcePool -> dockerClientFinder.findProperDockerClient(resourcePool))
                .collect(Collectors.toSet());
        distinctDockerClients.forEach(dockerClient -> {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withLabelFilter(RunExecutorDockerImpl.CONTAINER_LABELS).withShowAll(true).exec();
            containers.forEach(container -> reportRun(container));
        });

    }

    public void reportRun(Container c) {
        runReportReceiver.receive(containerToTaskReport(c));
    }

    @NotNull
    private ReportedRun containerToTaskReport(Container c) {
        RunStatus status = containerStatusExplainer.statusOf(c);
        Long stopMilli = status == RunStatus.FAILED || status == RunStatus.FINISHED ? System.currentTimeMillis() : null;
        String failReason = RunStatus.FAILED == status ? c.getStatus() : null;
        return ReportedRun.builder()
                .id(containerRunMapper.runIdOfContainer(c))
                .status(status)
                .ip(null)
                .startTimeMillis(null)
                .stopTimeMillis(stopMilli)
                .failedReason(failReason)
                .build();
    }

}
