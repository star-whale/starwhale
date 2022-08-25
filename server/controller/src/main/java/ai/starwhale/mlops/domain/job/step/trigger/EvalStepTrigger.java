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

package ai.starwhale.mlops.domain.job.step.trigger;

import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.split.JobSpliteratorEvaluation;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.storage.StorageAccessService;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
// TODO: common step trigger
public class EvalStepTrigger implements StepTrigger {

    final StorageAccessService storageAccessService;

    final TaskMapper taskMapper;

    public EvalStepTrigger(StorageAccessService storageAccessService,
                           TaskMapper taskMapper) {
        this.storageAccessService = storageAccessService;
        this.taskMapper = taskMapper;
    }

    public void triggerNextStep(Step pplStep) {
        Step nextStep = pplStep.getNextStep();
        List<Task> nextStepTasks = nextStep.getTasks();
        for (Task nextStepTask : nextStepTasks) {
            nextStepTask.updateStatus(TaskStatus.READY);
        }
    }

    public boolean applyTo(JobType jobType, String stepName) {
        return jobType == JobType.EVALUATION && JobSpliteratorEvaluation.STEP_NAMES[0].equals(stepName);
    }
}
