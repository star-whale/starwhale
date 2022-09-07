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

package ai.starwhale.mlops.domain.job.cache;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.trigger.StepTrigger;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.WatchableTaskFactory;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * load job to JobHolder
 */
@Slf4j
@Service
public class JobLoader {

    final SwTaskScheduler swTaskScheduler;

    final TaskMapper taskMapper;

    final TaskBoConverter taskBoConverter;

    final JobBoConverter jobBoConverter;

    final HotJobHolder jobHolder;

    final StepMapper stepMapper;

    final StepConverter stepConverter;

    final WatchableTaskFactory watchableTaskFactory;

    final StepTrigger stepTrigger;

    final StepHelper stepHelper;

    final JobUpdateHelper jobUpdateHelper;

    public JobLoader(SwTaskScheduler swTaskScheduler,
            TaskMapper taskMapper, TaskBoConverter taskBoConverter,
            JobBoConverter jobBoConverter, HotJobHolder jobHolder,
            StepMapper stepMapper, StepConverter stepConverter,
            WatchableTaskFactory watchableTaskFactory,
            StepTrigger stepTrigger, StepHelper stepHelper,
            JobUpdateHelper jobUpdateHelper) {
        this.swTaskScheduler = swTaskScheduler;
        this.taskMapper = taskMapper;
        this.taskBoConverter = taskBoConverter;
        this.jobBoConverter = jobBoConverter;
        this.jobHolder = jobHolder;
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.watchableTaskFactory = watchableTaskFactory;
        this.stepTrigger = stepTrigger;
        this.stepHelper = stepHelper;
        this.jobUpdateHelper = jobUpdateHelper;
    }

    public List<Job> loadEntities(List<JobEntity> jobEntityList, Boolean resumePausedOrFailTasks, Boolean doCache) {
        if (CollectionUtils.isEmpty(jobEntityList)) {
            return new ArrayList<>(0);
        }
        if (resumePausedOrFailTasks && !doCache) {
            throw new SwProcessException(ErrorType.SYSTEM).tip(
                    "unsupported params combination: resumePausedOrFailTasks:true & doCache:false");
        }
        return jobEntityList.parallelStream()
                .map(jobBoConverter::fromEntity)
                .peek(job -> {
                    fillStepsAndTasks(job, resumePausedOrFailTasks, doCache);
                    if (doCache) {
                        jobHolder.adopt(job);
                    }
                })
                .collect(Collectors.toList());

    }

    private void fillStepsAndTasks(Job job, Boolean resumePausedOrFailTasks, Boolean doCache) {
        List<StepEntity> stepEntities = stepMapper.findByJobId(job.getId());
        List<Step> steps = stepEntities.parallelStream().map(stepConverter::fromEntity)
                .peek(step -> {
                    log.debug("start");
                    step.setJob(job);
                    List<TaskEntity> taskEntities = taskMapper.findByStepId(step.getId());
                    List<Task> tasks = taskBoConverter.fromTaskEntity(taskEntities, step); // PAUSED, FAIL
                    if (resumePausedOrFailTasks) {
                        resumeFrozenTasks(tasks);
                    }
                    log.debug("start cache");
                    if (doCache) {
                        List<Task> watchableTasks = watchableTaskFactory.wrapTasks(tasks);
                        scheduleReadyTasks(watchableTasks.parallelStream()
                                .filter(t -> t.getStatus() == TaskStatus.READY)
                                .collect(
                                        Collectors.toSet()));
                        step.setTasks(watchableTasks);
                    } else {
                        step.setTasks(tasks);
                    }

                    log.debug("start set status");
                    step.setStatus(stepHelper.desiredStepStatus(tasks.parallelStream().map(Task::getStatus).collect(
                            Collectors.toSet())));
                    if (step.getStatus() == StepStatus.RUNNING) {
                        if (job.getCurrentStep() != null) {
                            log.error("FATAL!!!!! A job has two running steps job id: {}", job.getId());
                        }
                        job.setCurrentStep(step);
                    }
                }).collect(Collectors.toList());
        linkSteps(steps, stepEntities);
        job.setSteps(steps);
        if (null == job.getCurrentStep() && doCache) {
            triggerPossibleNextStep(job);
        }
        jobUpdateHelper.updateJob(job);
    }

    private void resumeFrozenTasks(List<Task> tasks) {
        tasks.parallelStream().filter(t -> t.getStatus() == TaskStatus.PAUSED
                        || t.getStatus() == TaskStatus.FAIL
                        || t.getStatus() == TaskStatus.CANCELED)
                .forEach(t -> t.updateStatus(TaskStatus.READY));
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

    /**
     * load READY tasks on start
     */
    void scheduleReadyTasks(Collection<Task> tasks) {
        if (null == tasks) {
            return;
        }
        tasks.parallelStream()
                .collect(Collectors.groupingBy(task -> task.getStep().getJob().getJobRuntime().getDeviceClass()))
                .forEach((deviceClass, taskList) ->
                        swTaskScheduler.schedule(taskList, deviceClass));
    }

    private void triggerPossibleNextStep(Job job) {
        log.warn("a job shall has a current step after fill steps job id: {} trying to trigger one", job.getId());
        Step stepPointer = stepHelper.firsStep(job.getSteps());
        do {
            if (stepPointer.getStatus() == StepStatus.SUCCESS) {
                Step nextStep = stepPointer.getNextStep();
                if (null == nextStep) {
                    break;
                }
                if (nextStep.getStatus() == StepStatus.CREATED) {
                    stepTrigger.triggerNextStep(stepPointer);
                    break;
                }
            } else {
                log.warn("step is not success and is not job current status {} id:{}", stepPointer.getStatus(),
                        stepPointer.getId());
            }
            stepPointer = stepPointer.getNextStep();

        } while (null != stepPointer);
    }

}
