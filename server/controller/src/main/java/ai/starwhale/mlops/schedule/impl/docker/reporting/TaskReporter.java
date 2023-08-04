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
import ai.starwhale.mlops.schedule.impl.docker.ContainerTaskMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.impl.docker.SwTaskSchedulerDocker;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

@Slf4j
public class TaskReporter {

    TaskReportReceiver taskReportReceiver;

    final SystemSettingService systemSettingService;

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    public TaskReporter(SystemSettingService systemSettingService, DockerClientFinder dockerClientFinder,
            ContainerTaskMapper containerTaskMapper) {
        this.systemSettingService = systemSettingService;
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 3000)
    public void reportTasks(){
        if(null == this.taskReportReceiver){
            log.warn("taskReportReceiver hasn't been set yet");
            return;
        }

        List<ResourcePool> resourcePools = systemSettingService.getAllResourcePools();
        if(CollectionUtils.isEmpty(resourcePools)){
            resourcePools = List.of(null);
        }
        resourcePools.forEach(resourcePool -> {
            DockerClient dockerClient = dockerClientFinder.findProperDockerClient(resourcePool);
            List<Container> containers = dockerClient.listContainersCmd()
                    .withLabelFilter(SwTaskSchedulerDocker.CONTAINER_LABELS).exec();
            taskReportReceiver.receive(containers.stream().map(c->{
                Long taskId = containerTaskMapper.taskIfOfContainer(c.getNames()[0]);
                c.getState();
                c.getStatus();
                return new ReportedTask(taskId, null, 0, null, 0L, 0L, "");
            }).collect(Collectors.toList()));
        });

    }

    public void setTaskReportReceiverIfNotSet(TaskReportReceiver taskReportReceiver){
        if(null != this.taskReportReceiver){
            return;
        }
        this.taskReportReceiver = taskReportReceiver;
    }

}
