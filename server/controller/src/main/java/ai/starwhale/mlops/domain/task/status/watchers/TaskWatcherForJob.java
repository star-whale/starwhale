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

package ai.starwhale.mlops.domain.task.status.watchers;

import ai.starwhale.mlops.domain.dag.DAGEditor;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.task.cache.LivingTaskCache;
import ai.starwhale.mlops.domain.task.TaskJobStatusHelper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * task status change for job status change
 */
@Slf4j
@Component
@Order(3)
public class TaskWatcherForJob implements TaskStatusChangeWatcher {

    final LivingTaskCache taskCache;

    final TaskJobStatusHelper taskJobStatusHelper;

    final JobStatusMachine jobStatusMachine;

    final JobMapper jobMapper;

    final SWTaskScheduler swTaskScheduler;

    final TaskMapper taskMapper;

    final DAGEditor dagEditor;

    public TaskWatcherForJob(@Qualifier("cacheWrapperReadOnly") LivingTaskCache taskCache,
        TaskJobStatusHelper taskJobStatusHelper,
        JobStatusMachine jobStatusMachine, JobMapper jobMapper,
        SWTaskScheduler swTaskScheduler,
        TaskMapper taskMapper, DAGEditor dagEditor) {
        this.taskCache = taskCache;
        this.taskJobStatusHelper = taskJobStatusHelper;
        this.jobStatusMachine = jobStatusMachine;
        this.jobMapper = jobMapper;
        this.swTaskScheduler = swTaskScheduler;
        this.taskMapper = taskMapper;
        this.dagEditor = dagEditor;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus newStatus) {
        Job job = task.getJob();
        if(jobStatusMachine.isFinal(job.getStatus())){
            return;
        }
        synchronized (job){
            Collection<Task> tasks = taskCache.ofJob(job.getId());
            JobStatus jobNewStatus = taskJobStatusHelper.desiredJobStatus(tasks);
            if(job.getStatus() == jobNewStatus){
                return;
            }
            if(!jobStatusMachine.couldTransfer(job.getStatus(),jobNewStatus)){
                log.error("job status change unexpectedly from {} to {}",job.getStatus(),jobNewStatus);
                return;
            }
            log.info("job status change from {} to {} with id {}",job.getStatus(),jobNewStatus,job.getId());
            job.setStatus(jobNewStatus);
            dagEditor.jobStatusChange(job,jobNewStatus);
            jobMapper.updateJobStatus(List.of(job.getId()),jobNewStatus);
            if(jobStatusMachine.isFinal(jobNewStatus)){
                jobMapper.updateJobFinishedTime(List.of(job.getId()), Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime());
                taskCache.clearTasksOf(job.getId());
            }
            if(jobNewStatus == JobStatus.FAIL){
                log.info("tasks stopped schedule because of failed {}",job.getId());
                swTaskScheduler.stopSchedule(tasks.parallelStream().filter(t->t.getStatus() == TaskStatus.CREATED).map(Task::getId).collect(
                    Collectors.toList()));
            }
        }
    }
}
