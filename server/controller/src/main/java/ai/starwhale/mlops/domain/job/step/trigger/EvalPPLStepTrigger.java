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
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
// TODO: common step trigger
public class EvalPPLStepTrigger implements StepTrigger{

    final StorageAccessService storageAccessService;

    final TaskMapper taskMapper;

    public EvalPPLStepTrigger(StorageAccessService storageAccessService,
        TaskMapper taskMapper) {
        this.storageAccessService = storageAccessService;
        this.taskMapper = taskMapper;
    }

    public void triggerNextStep(Step pplStep){
        List<Task> pplTasks = pplStep.getTasks();
        Step cmpStep = pplStep.getNextStep();
        List<Task> cmpTasks = cmpStep.getTasks();
        if(null == cmpTasks || cmpTasks.isEmpty()){
            log.error("FATAL !!!! CMP Tasks shall exists of cmpStep {} but no",cmpStep.getId());
            return;
        }
        if(cmpTasks.size() > 1){
            log.error("FATAL !!!! CMP Tasks shall only have one task  {} but got {}",cmpStep.getId(),cmpTasks.size());
            return;
        }
        Task cmpTask = cmpTasks.get(0);
        List<String> allPPLTaskResults = pplTasks.parallelStream().flatMap(task -> {
                try {
                    return storageAccessService.list(task.getResultRootPath().resultDir());
                } catch (IOException e) {
                    throw new SWProcessException(ErrorType.STORAGE).tip("list task result dir failed");
                }
            })
            .collect(Collectors.toList());
        CMPRequest cmpRequest = new CMPRequest(allPPLTaskResults);
        taskMapper.updateTaskRequest(cmpTask.getId(),cmpRequest.toString());
        cmpTask.setTaskRequest(cmpRequest);
        cmpTask.updateStatus(TaskStatus.READY);
    }
    public boolean applyTo(JobType jobType,String stepName){
        return jobType == JobType.EVALUATION && JobSpliteratorEvaluation.STEP_NAMES[0].equals(stepName);
    }
}
