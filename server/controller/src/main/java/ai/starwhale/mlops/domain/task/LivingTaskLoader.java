/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.JobMapper;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.TaskScheduler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

/**
 * loading tasks
 */
public class LivingTaskLoader {

    LivingTaskStatusMachine livingTaskStatusMachine;

    TaskScheduler taskScheduler;

    CommandingTasksChecker commandingTasksChecker;

    TaskMapper taskMapper;

    JobMapper jobMapper;

    TaskBoConverter taskBoConverter;

    JobBoConverter jobBoConverter;

    /**
     * load tasks that are not FINISHED or ERROR into mem
     */
    @PostConstruct
    public void loadLivingTasks(){
        //load living tasks and assign them to livingTaskStatusMachine
        Stream<TaskEntity> taskStream = livingTasksFromDB();
        final Map<Integer, List<TaskEntity>> collect = taskStream.parallel()
            .collect(Collectors.groupingBy(TaskEntity::getTaskStatus));
        collect.entrySet().parallelStream()
            .forEach(entry -> livingTaskStatusMachine.adopt(entry.getValue().stream().map(entity->taskBoConverter.fromTaskEntity(entity)).collect(
                Collectors.toList()), TaskStatus.from(entry.getKey())));

        scheduleCreatedTasks(collect.get(TaskStatus.CREATED));
        checkCommandingTasks(collect.get(TaskStatus.ASSIGNING));
        checkCommandingTasks(collect.get(TaskStatus.CANCEL_COMMANDING));
    }

    /**
     * @return tasks that are not FINISHED or ERROR
     */
    private Stream<TaskEntity> livingTasksFromDB() {
        final List<Integer> livingTaskStatus = Arrays.asList(TaskStatus.values())
            .parallelStream()
            .filter(taskStatus -> taskStatus != TaskStatus.FINISHED
                && taskStatus != TaskStatus.ARCHIVED
                && taskStatus != TaskStatus.CANCELED
                && taskStatus != TaskStatus.EXIT_ERROR)
            .map(TaskStatus::getOrder)
            .collect(Collectors.toList());
        return taskMapper.findTaskByStatusIn(livingTaskStatus).stream();
    }


    /**
     * load CREATED tasks on start
     */
    void scheduleCreatedTasks(List<TaskEntity> tasks){
        if(null == tasks){
            return;
        }
        tasks.parallelStream().collect(Collectors.groupingBy(TaskEntity::getJobId))
            .forEach((jobId, taskList) -> {
                final JobEntity job = jobMapper.findJobById(jobId);
                taskScheduler.adoptTasks(
                    taskBoConverter.toTaskTrigger(taskList, jobBoConverter.fromEntity(job)),
                    Device.Clazz.from(job.getDeviceType()));
            });

    }

    /**
     * load commanding tasks on start
     */
    void checkCommandingTasks(List<TaskEntity> tasks){
        if(null == tasks){
            return;
        }
        tasks.parallelStream()
            .collect(Collectors.groupingBy(TaskEntity::getAgent))
            .forEach((agent, taskEntities) -> commandingTasksChecker
                .onTaskCommanding(taskBoConverter.toTaskCommand(taskEntities),
                    Node.builder().ipAddr(agent.getAgentIp()).build()));

    }

}
