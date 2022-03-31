/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.TaskScheduler;
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

    /**
     * load tasks that are not FINISHED or ERROR into mem
     */
    @PostConstruct
    public void loadLivingTasks(){
        //load living tasks and assign them to livingTaskStatusMachine
        Stream<Task> taskStream = livingTasksInDB();
        final Map<TaskStatus, List<Task>> collect = taskStream.parallel()
            .collect(Collectors.groupingBy(Task::getStatus));
        collect.entrySet().parallelStream()
            .forEach(entry -> livingTaskStatusMachine.adopt(entry.getValue(), entry.getKey()));

        scheduleCreatedTasks(collect.get(TaskStatus.CREATED));
        checkCommandingTasks(collect.get(TaskStatus.ASSIGNING));
        checkCommandingTasks(collect.get(TaskStatus.CANCEL_COMMANDING));
    }

    /**
     * @return tasks that are not FINISHED or ERROR
     */
    private Stream<Task> livingTasksInDB() {
        //TODO
        return null;
    }


    /**
     * load CREATED tasks on start
     */
    void scheduleCreatedTasks(List<Task> tasks){
        //TODO wrap task to task trigger
        taskScheduler.adoptTasks(null,null);
    }

    /**
     * load commanding tasks on start
     */
    void checkCommandingTasks(List<Task> tasks){
        //TODO ASSIGNING & CANCEL_COMMANDING load from DB. wrap task to task command
        commandingTasksChecker.onTaskCommanding(null,null);
    }



}
