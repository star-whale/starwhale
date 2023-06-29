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

package ai.starwhale.mlops.domain.job.step;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.status.StepStatusMachine;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StepService {
    private final StepMapper stepMapper;
    private final StepConverter stepConverter;

    private final TaskService taskService;

    public StepService(StepMapper stepMapper, StepConverter stepConverter, TaskService taskService) {
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.taskService = taskService;
    }

    public void insert(StepEntity entity) {
        stepMapper.insert(entity);
    }

    public void updateLastStep(Long id, Long lastId) {
        stepMapper.updateLastStep(id, lastId);
    }

    public void fillJobSteps(Job job) {
        var entities = stepMapper.findByJobId(job.getId());
        var steps = entities.stream().map(entity -> {
            try {
                var step = stepConverter.fromEntity(entity);
                if (step.getResourcePool() == null) {
                    // backward compatibility
                    step.setResourcePool(job.getResourcePool());
                }
                return step;
            } catch (IOException e) {
                log.error("can not convert step entity to step", e);
                return null;
            }
        }).filter(Objects::nonNull).peek(step -> {
            step.setJob(job);
            taskService.fillStepTasks(step);
            if (step.getStatus() == StepStatus.RUNNING) {
                if (job.getCurrentStep() != null) {
                    log.error("ERROR!!!!! A job has two running steps job id: {}", job.getId());
                }
                job.setCurrentStep(step);
            }
        }).collect(Collectors.toList());
        linkSteps(steps, entities);
        job.setSteps(steps);
    }

    private void linkSteps(List<Step> steps, List<StepEntity> stepEntities) {
        Map<Long, Step> stepMap = steps.parallelStream()
                .collect(Collectors.toMap(Step::getId, Function.identity()));
        Map<Long, Long> linkMap = stepEntities.parallelStream()
                .filter(stepEntity -> null != stepEntity.getLastStepId())
                .collect(Collectors.toMap(StepEntity::getLastStepId, StepEntity::getId));
        steps.forEach(step -> {
            Long nextStepId = linkMap.get(step.getId());
            if (null == nextStepId) {
                return;
            }
            step.setNextStep(stepMap.get(nextStepId));
        });
    }

    public void updateStepStatus(Step step, StepStatus newStatus) {
        log.info("step status change from {} to {} with id {}", step.getStatus(), newStatus, step.getId());
        step.setStatus(newStatus);
        stepMapper.updateStatus(List.of(step.getId()), newStatus);
        var now = new Date();
        if (StepStatusMachine.isFinal(newStatus)) {
            step.setFinishTime(now.getTime());
            stepMapper.updateFinishedTime(step.getId(), now);
        }
        if (StepStatus.RUNNING == newStatus) {
            step.setStartTime(now.getTime());
            stepMapper.updateStartedTime(step.getId(), now);
        }
    }

    public Step firsStep(List<Step> linkedSteps) {
        List<Step> followingSteps = linkedSteps.stream().map(Step::getNextStep).filter(
                        Objects::nonNull)
                .collect(Collectors.toList());
        Optional<Step> headStepOp = linkedSteps.stream().filter(step -> !followingSteps.contains(step))
                .findAny();
        return headStepOp.get();
    }


}
