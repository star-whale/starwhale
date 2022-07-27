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

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * the processor for every report from Agent
 */
@Slf4j
@Service
public class TaskStatusReceiverFromK8S implements TaskStatusReceiver {

    final K8sClient k8sClient;

    final HotJobHolder jobHolder;

    final TaskMapper taskMapper;

    public TaskStatusReceiverFromK8S(K8sClient k8sClient,
        HotJobHolder jobHolder, TaskMapper taskMapper) {
        this.k8sClient = k8sClient;
        this.jobHolder = jobHolder;
        this.taskMapper = taskMapper;
    }

    @Scheduled(fixedDelayString = "${sw.controller.task.schedule.fixedDelay.in.milliseconds:1000}")
    public void watchK8S()  {
        V1JobList jobList = new V1JobList();
        try {
            jobList = k8sClient.get();
        } catch (ApiException e) {
            log.error(e.getMessage());
        }

        // fetch job status
        List<V1Job> done = new ArrayList<>();
        List<V1Job> undone = new ArrayList<>();
        jobList.getItems().forEach(j -> {
            if (j.getStatus() == null) {
                undone.add(j);
            }
            if (j.getStatus().getActive() == null) {
                done.add(j);
            } else {
                undone.add(j);
            }
        });

        // report task
        List<ReportedTask> reports = done.stream().map(i -> {
            Long id = Long.parseLong(i.getMetadata().getName());
            return new ReportedTask(id, TaskStatus.SUCCESS, TaskType.PPL);
        }).collect(Collectors.toList());

        receive(reports);

    }

    @Override
    public void receive(List<ReportedTask> reportedTasks) {

        reportedTasks.forEach(reportedTask -> {
            Collection<Task> optionalTasks = jobHolder.tasksOfIds(List.of(reportedTask.getId()));
            if(null == optionalTasks || optionalTasks.isEmpty()){
                log.warn("un-cached tasks reported {}, status directly update to DB",reportedTask.getId());
                taskMapper.updateTaskStatus(List.of(reportedTask.getId()),reportedTask.getStatus());
                return;
            }
            optionalTasks.forEach(task -> task.updateStatus(reportedTask.getStatus()));
        });

    }
}
