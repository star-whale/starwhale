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

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.trigger.StepTriggerContext;
import ai.starwhale.mlops.domain.task.StepHelper;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.WatchableTaskFactory;
import ai.starwhale.mlops.schedule.CommandingTasksAssurance;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * loading hot jobs
 */
@Service
@Order(2)
@Slf4j
public class HotJobsLoader implements CommandLineRunner {

    final SWTaskScheduler swTaskScheduler;

    final CommandingTasksAssurance commandingTasksAssurance;

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final TaskBoConverter taskBoConverter;

    final JobBoConverter jobBoConverter;

    final JobStatusMachine jobStatusMachine;

    final HotJobHolder jobHolder;

    final StepMapper stepMapper;

    final StepConverter stepConverter;

    final WatchableTaskFactory watchableTaskFactory;

    final StepTriggerContext stepTriggerContext;

    final StepHelper stepHelper;

    final JobUpdateHelper jobUpdateHelper;

    public HotJobsLoader(
        SWTaskScheduler swTaskScheduler, CommandingTasksAssurance commandingTasksAssurance,
        TaskMapper taskMapper, JobMapper jobMapper, TaskBoConverter taskBoConverter,
        JobBoConverter jobBoConverter,
        JobStatusMachine jobStatusMachine, HotJobHolder jobHolder,
        StepMapper stepMapper, StepConverter stepConverter,
        WatchableTaskFactory watchableTaskFactory,
        StepTriggerContext stepTriggerContext, StepHelper stepHelper,
        JobUpdateHelper jobUpdateHelper) {
        this.swTaskScheduler = swTaskScheduler;
        this.commandingTasksAssurance = commandingTasksAssurance;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskBoConverter = taskBoConverter;
        this.jobBoConverter = jobBoConverter;
        this.jobStatusMachine = jobStatusMachine;
        this.jobHolder = jobHolder;
        this.stepMapper = stepMapper;
        this.stepConverter = stepConverter;
        this.watchableTaskFactory = watchableTaskFactory;
        this.stepTriggerContext = stepTriggerContext;
        this.stepHelper = stepHelper;
        this.jobUpdateHelper = jobUpdateHelper;
    }

    /**
     * load jobs that are not FINISHED/ERROR/CANCELED/CREATED into mem
     * CREATED job has no steps, so it will not be loaded here
     */
    void loadHotJobs(){
        List<Job> hotJobs = hotJobsFromDB();
        hotJobs.parallelStream().forEach(job -> {
            fillStepsAndTasks(job);
            jobHolder.adopt(job);
            jobUpdateHelper.updateJob(job);
        });
    }

    private void fillStepsAndTasks(Job job) {
        List<StepEntity> stepEntities = stepMapper.findByJobId(job.getId());
        List<Step> steps = stepEntities.parallelStream().map(stepConverter::fromEntity)
            .peek(step -> {
                step.setJob(job);
                List<TaskEntity> taskEntities = taskMapper.findByStepId(step.getId());
                List<Task> tasks = taskBoConverter.fromTaskEntity(taskEntities, step);
                List<Task> watchableTasks = watchableTaskFactory.wrapTasks(tasks);
                scheduleReadyTasks(watchableTasks.parallelStream()
                    .filter(t -> t.getStatus() == TaskStatus.READY)
                    .collect(
                        Collectors.toSet()));
                assureCommandingTasks(watchableTasks.parallelStream().filter(
                    t -> t.getStatus() == TaskStatus.ASSIGNING
                        || t.getStatus() == TaskStatus.CANCELLING).collect(
                    Collectors.toSet()));
                step.setTasks(watchableTasks);
                step.setStatus(stepHelper.desiredStepStatus(tasks.parallelStream().map(Task::getStatus).collect(
                    Collectors.toSet())));
                if(step.getStatus() == StepStatus.RUNNING){
                    if(job.getCurrentStep() != null){
                        log.error("FATAL!!!!! A job has two running steps job id: {}",job.getId());
                    }
                    job.setCurrentStep(step);
                }
            }).collect(Collectors.toList());
        linkSteps(steps,stepEntities);
        job.setSteps(steps);
        if(null == job.getCurrentStep()){
            triggerPossibleNextStep(job);
        }
    }

    private void triggerPossibleNextStep(Job job) {
        log.warn("a job shall has a current step after fill steps job id: {} trying to trigger one", job.getId());
        Step stepPointer = stepHelper.firsStep(job.getSteps());
        do{
            if(stepPointer.getStatus() == StepStatus.SUCCESS){
                Step nextStep = stepPointer.getNextStep();
                if(null == nextStep){
                    break;
                }
                if(nextStep.getStatus() == StepStatus.CREATED){
                    stepTriggerContext.triggerNextStep(stepPointer);
                }
            }else{
                log.warn("step is not success and is not job current status {} id:{}",stepPointer.getStatus(),stepPointer.getId());
            }
            stepPointer = stepPointer.getNextStep();

        }while (null != stepPointer);
    }

    private void linkSteps(List<Step> steps, List<StepEntity> stepEntities) {
        Map<Long, Step> stepMap = steps.parallelStream()
            .collect(Collectors.toMap(Step::getId, Function.identity()));
        Map<Long, Long> linkMap = stepEntities.parallelStream()
            .filter(stepEntity -> null != stepEntity.getLastStepId())
            .collect(Collectors.toMap(StepEntity::getLastStepId, StepEntity::getId));
        steps.forEach(step -> {
            Long nextStepId = linkMap.get(step.getId());
            if(null == nextStepId){
                return;
            }
            step.setNextStep(stepMap.get(nextStepId));
        });
    }

    /**
     * @return tasks of jobs that are not FINISHED nor ERROR nor CREATED
     */
    private List<Job> hotJobsFromDB() {
        List<JobStatus> hotJobStatuses = Arrays.asList(JobStatus.values())
            .parallelStream()
            .filter(jobStatus -> jobStatusMachine.isHot(jobStatus))
            .collect(Collectors.toList());
        return jobMapper.findJobByStatusIn(hotJobStatuses)
            .parallelStream()
            .map(jobBoConverter::fromEntity)
            .collect(Collectors.toList());
    }


    /**
     * load READY tasks on start
     */
    void scheduleReadyTasks(Collection<Task> tasks){
        if (null == tasks) {
            return;
        }
        tasks.parallelStream()
            .collect(Collectors.groupingBy(task -> task.getStep().getJob().getJobRuntime().getDeviceClass()))
            .forEach((deviceClass, taskList) ->
                swTaskScheduler.adoptTasks(taskList, deviceClass));
    }

    /**
     * assure commanding tasks on start
     */
    void assureCommandingTasks(Collection<Task> tasks){
        if(null == tasks){
            return;
        }
        tasks.parallelStream()
            .filter(task -> null != task.getAgent())
            .collect(Collectors.groupingBy(Task::getAgent))
            .forEach((agent, taskList) -> commandingTasksAssurance
                .onTaskCommanding(taskBoConverter.toTaskCommand(taskList),agent));

    }

    @Override
    public void run(String... args) throws Exception {
        loadHotJobs();
        log.info("living jobs loaded");
    }
}
