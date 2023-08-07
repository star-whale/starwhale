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

import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.schedule.impl.docker.ContainerTaskMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.impl.docker.SwTaskSchedulerDocker;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DockerTaskReporter {

    final TaskReportReceiver taskReportReceiver;

    final SystemSettingService systemSettingService;

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    final ContainerStatusExplainer containerStatusExplainer;

    final TaskStatusMachine taskStatusMachine;

    public DockerTaskReporter(TaskReportReceiver taskReportReceiver, SystemSettingService systemSettingService,
            DockerClientFinder dockerClientFinder,
            ContainerTaskMapper containerTaskMapper, ContainerStatusExplainer containerStatusExplainer,
            TaskStatusMachine taskStatusMachine) {
        this.taskReportReceiver = taskReportReceiver;
        this.systemSettingService = systemSettingService;
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
        this.containerStatusExplainer = containerStatusExplainer;
        this.taskStatusMachine = taskStatusMachine;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 3000)
    public void reportTasks() {

        List<ResourcePool> resourcePools = systemSettingService.getAllResourcePools();
        if (CollectionUtils.isEmpty(resourcePools)) {
            resourcePools = List.of(null);
        }
        resourcePools.forEach(resourcePool -> {
            DockerClient dockerClient = dockerClientFinder.findProperDockerClient(resourcePool);
            List<Container> containers = dockerClient.listContainersCmd()
                    .withLabelFilter(SwTaskSchedulerDocker.CONTAINER_LABELS).withShowAll(true).exec();
            taskReportReceiver.receive(containers.stream().map(c -> containerToTaskReport(c)).filter(Objects::nonNull).collect(Collectors.toList()));
        });

    }

    public void reportTask(Container c){
        taskReportReceiver.receive(List.of(containerToTaskReport(c)));
    }

    @Nullable
    private ReportedTask containerToTaskReport(Container c) {
        Long taskId;
        try {
            taskId = containerTaskMapper.taskIfOfContainer(c.getNames()[0]);
        } catch (SwValidationException e) {
            log.warn("malformat container name found {}", c.getNames()[0]);
            return null;
        }

        TaskStatus status = containerStatusExplainer.statusOf(c, taskId);
        Long stopMilli = taskStatusMachine.isFinal(status) ? System.currentTimeMillis() : null;
        String failReason = TaskStatus.FAIL == status ? c.getStatus() : null;
        return new ReportedTask(taskId, status, 0, null, null, stopMilli, failReason);
    }

}
