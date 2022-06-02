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
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
    final SWTaskScheduler swTaskScheduler;
    final LocalDateTimeConvertor localDateTimeConvertor;

    public JobUpdateHelper(HotJobHolder jobHolder,
        JobStatusCalculator jobStatusCalculator,
        JobMapper jobMapper, JobStatusMachine jobStatusMachine,
        SWTaskScheduler swTaskScheduler,
        LocalDateTimeConvertor localDateTimeConvertor) {
        this.jobHolder = jobHolder;
        this.jobStatusCalculator = jobStatusCalculator;
        this.jobMapper = jobMapper;
        this.jobStatusMachine = jobStatusMachine;
        this.swTaskScheduler = swTaskScheduler;
        this.localDateTimeConvertor = localDateTimeConvertor;
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
            log.warn("job status change unexpectedly from {} to {} of id {} is forbidden",
                currentStatus, desiredJobStatus, job.getId());
            return;
        }
        log.info("job status change from {} to {} with id {}", currentStatus, desiredJobStatus,
            job.getId());
        job.setStatus(desiredJobStatus);
        jobMapper.updateJobStatus(List.of(job.getId()), desiredJobStatus);

        if (jobStatusMachine.isFinal(desiredJobStatus)) {
            log.info("job removed from JobHolder because status is {} job id: {}",job.getStatus(),job.getId());
            jobHolder.remove(job.getId());
            jobMapper.updateJobFinishedTime(List.of(job.getId()),localDateTimeConvertor.revert(System.currentTimeMillis()));
            if (desiredJobStatus == JobStatus.FAIL) {
                log.info("tasks stopped schedule because of job failed {}", job.getId());
                swTaskScheduler.stopSchedule(
                    job.getSteps().stream()
                    .map(Step::getTasks)
                        .flatMap(Collection::parallelStream)
                        .filter(t -> t.getStatus() == TaskStatus.READY).map(Task::getId)
                        .collect(Collectors.toList())
                );
            }
        }

    }
}
