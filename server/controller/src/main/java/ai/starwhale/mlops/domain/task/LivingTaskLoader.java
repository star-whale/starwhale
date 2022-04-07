/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.JobMapper;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.TaskStatusStage;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

/**
 * loading tasks
 */
@Service
public class LivingTaskLoader {

    final LivingTaskStatusMachine livingTaskStatusMachine;

    final TaskScheduler taskScheduler;

    final CommandingTasksChecker commandingTasksChecker;

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final TaskBoConverter taskBoConverter;

    final JobBoConverter jobBoConverter;

    public LivingTaskLoader(LivingTaskStatusMachine livingTaskStatusMachine, TaskScheduler taskScheduler, CommandingTasksChecker commandingTasksChecker, TaskMapper taskMapper, JobMapper jobMapper, TaskBoConverter taskBoConverter, JobBoConverter jobBoConverter) {
        this.livingTaskStatusMachine = livingTaskStatusMachine;
        this.taskScheduler = taskScheduler;
        this.commandingTasksChecker = commandingTasksChecker;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.taskBoConverter = taskBoConverter;
        this.jobBoConverter = jobBoConverter;
    }

    /**
     * load tasks that are not FINISHED or ERROR into mem
     */
    @PostConstruct
    public void loadLivingTasks(){
        //load living tasks and assign them to livingTaskStatusMachine
        Stream<TaskEntity> taskStream = livingTasksFromDB();
        final Map<Long, List<TaskEntity>> collectJob = taskStream.parallel()
            .collect(Collectors.groupingBy(TaskEntity::getJobId));
        final Map<StagingTaskStatus, List<Task>> collectStatus = collectJob.entrySet().parallelStream()
            .map(entry -> {
                final JobEntity job = jobMapper.findJobById(entry.getKey());
                return taskBoConverter
                    .fromTaskEntity(entry.getValue(), jobBoConverter.fromEntity(job));
            }).flatMap(Collection::stream)
            .collect(Collectors.groupingBy(Task::getStatus));

        collectStatus.entrySet().parallelStream()
            .forEach(entry -> livingTaskStatusMachine.adopt(entry.getValue(), entry.getKey()));

        scheduleCreatedTasks(collectStatus.get(new StagingTaskStatus(TaskStatus.CREATED)));
        checkCommandingTasks(collectStatus.get(new StagingTaskStatus(TaskStatus.ASSIGNING)));
        checkCommandingTasks(collectStatus.get(new StagingTaskStatus(TaskStatus.CANCEL_COMMANDING)));
    }

    /**
     * @return tasks that are not FINISHED or ERROR
     */
    private Stream<TaskEntity> livingTasksFromDB() {
        final List<Integer> livingTaskStatus = Arrays.asList(TaskStatus.values())
            .parallelStream()
            .map(status-> List.of(new StagingTaskStatus(status,TaskStatusStage.INIT),new StagingTaskStatus(status,TaskStatusStage.DOING),new StagingTaskStatus(status,TaskStatusStage.DONE)))
            .flatMap(Collection::stream)
            .filter(taskStatus -> !taskStatus.equals(new StagingTaskStatus(TaskStatus.FINISHED,TaskStatusStage.DONE))
                && !taskStatus.equals(new StagingTaskStatus(TaskStatus.ARCHIVED))
                && !taskStatus.equals(new StagingTaskStatus(TaskStatus.CANCELED))
                && !taskStatus.equals(new StagingTaskStatus(TaskStatus.EXIT_ERROR)))
            .map(StagingTaskStatus::getValue)
            .collect(Collectors.toList());
        return taskMapper.findTaskByStatusIn(livingTaskStatus).stream();
    }


    /**
     * load CREATED tasks on start
     */
    void scheduleCreatedTasks(List<Task> tasks){
        if (null == tasks) {
            return;
        }
        tasks.parallelStream()
            .collect(Collectors.groupingBy(task -> task.getJob().getJobRuntime().getDeviceClass()))
            .forEach((deviceClass, taskList) ->
                taskScheduler.adoptTasks(taskList, deviceClass));
    }

    /**
     * load commanding tasks on start
     */
    void checkCommandingTasks(List<Task> tasks){
        if(null == tasks){
            return;
        }
        tasks.parallelStream()
            .collect(Collectors.groupingBy(Task::getAgent))
            .forEach((agent, taskList) -> commandingTasksChecker
                .onTaskCommanding(taskBoConverter.toTaskCommand(taskList),agent));

    }

}
