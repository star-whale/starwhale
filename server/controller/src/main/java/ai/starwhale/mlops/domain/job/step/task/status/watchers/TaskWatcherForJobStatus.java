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

package ai.starwhale.mlops.domain.job.step.task.status.watchers;

import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.StepService;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.status.StepStatusCalculator;
import ai.starwhale.mlops.domain.job.step.status.StepStatusMachine;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.trigger.StepTrigger;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * task status change for job status change
 */
@Slf4j
@Component
@Order(2)
public class TaskWatcherForJobStatus implements TaskStatusChangeWatcher {

    final StepService stepService;

    final StepTrigger stepTrigger;

    final JobService jobService;

    public TaskWatcherForJobStatus(
            StepService stepService,
            StepTrigger stepTrigger,
            @Lazy JobService jobService) {
        this.stepService = stepService;
        this.stepTrigger = stepTrigger;
        this.jobService = jobService;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus oldStatus) {
        Step step = task.getStep();
        Job job = step.getJob();
        log.debug("updating job {} status for task {}", job.getId(), task.getId());
        // TODO use KeyLock for future distributed and keyLock can be replaced with other impl
        synchronized (job) {
            log.debug("lock got for job {} and task {}", job.getId(), task.getId());
            Collection<TaskStatus> taskStatuses = step.getTasks()
                    .stream().map(Task::getStatus).collect(Collectors.toSet());
            StepStatus stepNewStatus = StepStatusCalculator.desiredStepStatus(taskStatuses);
            if (step.getStatus() == stepNewStatus) {
                log.debug("step status not changed {} id {}", stepNewStatus, step.getId());
                return;
            }
            if (!StepStatusMachine.couldTransfer(step.getStatus(), stepNewStatus)) {
                log.warn("step status change unexpectedly from {} to {} of id {} forbidden",
                        step.getStatus(), stepNewStatus, step.getId());
            }
            stepService.updateStepStatus(step, stepNewStatus);
            jobService.updateJob(job);
            if (step.getStatus() == StepStatus.SUCCESS && !job.isFinal()) {
                stepTrigger.triggerNextStep(step);
            }
        }
    }

}
