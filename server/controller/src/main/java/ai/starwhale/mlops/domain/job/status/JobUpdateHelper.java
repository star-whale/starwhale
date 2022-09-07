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

package ai.starwhale.mlops.domain.job.status;

import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForJobStatus;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobUpdateHelper {

    final HotJobHolder jobHolder;
    final JobStatusCalculator jobStatusCalculator;
    final JobMapper jobMapper;
    final JobStatusMachine jobStatusMachine;
    final SwTaskScheduler swTaskScheduler;
    final LocalDateTimeConvertor localDateTimeConvertor;
    final TaskStatusMachine taskStatusMachine;

    public JobUpdateHelper(HotJobHolder jobHolder,
            JobStatusCalculator jobStatusCalculator,
            JobMapper jobMapper, JobStatusMachine jobStatusMachine,
            SwTaskScheduler swTaskScheduler,
            LocalDateTimeConvertor localDateTimeConvertor,
            TaskStatusMachine taskStatusMachine) {
        this.jobHolder = jobHolder;
        this.jobStatusCalculator = jobStatusCalculator;
        this.jobMapper = jobMapper;
        this.jobStatusMachine = jobStatusMachine;
        this.swTaskScheduler = swTaskScheduler;
        this.localDateTimeConvertor = localDateTimeConvertor;
        this.taskStatusMachine = taskStatusMachine;
    }

    public void updateJob(Job job) {
        JobStatus currentStatus = job.getStatus();
        Set<StepStatus> stepStatuses = job.getSteps().stream().map(Step::getStatus)
                .collect(Collectors.toSet());
        JobStatus desiredJobStatus = jobStatusCalculator.desiredJobStatus(stepStatuses);
        if (currentStatus == desiredJobStatus) {
            log.debug("job status unchanged id:{} status:{}", job.getId(), job.getStatus());
            return;
        }
        if (!jobStatusMachine.couldTransfer(currentStatus, desiredJobStatus)) {
            log.warn("job status change unexpectedly from {} to {} of id {} ",
                    currentStatus, desiredJobStatus, job.getId());
        }
        log.info("job status change from {} to {} with id {}", currentStatus, desiredJobStatus,
                job.getId());
        job.setStatus(desiredJobStatus);
        jobMapper.updateJobStatus(List.of(job.getId()), desiredJobStatus);

        if (jobStatusMachine.isFinal(desiredJobStatus)) {
            jobMapper.updateJobFinishedTime(List.of(job.getId()),
                    localDateTimeConvertor.revert(System.currentTimeMillis()));
            if (desiredJobStatus == JobStatus.FAIL) {
                CompletableFuture.runAsync(() -> {
                    TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForJobStatus.class));
                    job.getSteps().stream().map(Step::getTasks).flatMap(Collection::stream)
                            .filter(task -> task.getStatus() == TaskStatus.RUNNING)
                            .forEach(task -> task.updateStatus(TaskStatus.CANCELED));
                    TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
                });
            }
            jobHolder.remove(job.getId());
        }
    }


    private void tryRemoveJobFromCache(Job job) {
        for (Step step : job.getSteps()) {
            for (Task task : step.getTasks()) {
                if (!taskStatusMachine.isFinal(task.getStatus()) && task.getStatus() != TaskStatus.CREATED) {
                    return;
                }
            }
        }
        jobHolder.remove(job.getId());
        log.info("job removed from JobHolder because all task is finished and job status is: {} job id is: {}",
                job.getStatus(), job.getId());

    }
}
