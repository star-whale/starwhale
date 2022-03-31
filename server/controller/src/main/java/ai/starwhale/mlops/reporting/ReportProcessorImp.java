/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportResponse;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.TaskScheduler;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.CollectionUtils;

/**
 * the processor for every report from Agent
 */
public class ReportProcessorImp implements ReportProcessor{

    CommandingTasksChecker commandingTasksChecker;

    LivingTaskStatusMachine livingTaskStatusMachine;

    TaskScheduler taskScheduler;

    TaskBoConverter taskBoConverter;

    // 1. check commanding tasks; 2. change task status; 3. schedule task & cancel task;
     public ReportResponse receive(ReportRequest report){
         final Node nodeInfo = report.getNodeInfo();
         final List<TaskCommand> unProperTasks = commandingTasksChecker
             .onNodeReporting(nodeInfo,report.getTasks());
         if(!CollectionUtils.isEmpty(unProperTasks)){
             return rebuildReportResponse(unProperTasks);
         }
         taskStatusChange(report.getTasks());
         final List<TaskTrigger> toAssignTasks = taskScheduler.schedule(nodeInfo);
         final Collection<Task> toCancelTasks = livingTaskStatusMachine.ofStatus(TaskStatus.TO_CANCEL);
         scheduledTaskStatusChange(toAssignTasks);
         canceledTaskStatusChange(toCancelTasks);
         commandingTasksChecker.onTaskCommanding(buildTaskCommands(toAssignTasks,toCancelTasks),nodeInfo);
         return buidResponse(toAssignTasks, toCancelTasks);
     }

    public ReportResponse buidResponse(List<TaskTrigger> toAssignTasks,
        Collection<Task> toCancelTasks) {
        final List<Long> taskIdsToCancel = toCancelTasks.stream().map(Task::getId)
            .collect(
                Collectors.toList());
        return new ReportResponse(
            taskIdsToCancel, toAssignTasks);
    }

    private void canceledTaskStatusChange(Collection<Task> tasks) {
        livingTaskStatusMachine.adopt(tasks,TaskStatus.CANCEL_COMMANDING);
    }

    private void scheduledTaskStatusChange(List<TaskTrigger> toAssignTasks) {
        livingTaskStatusMachine.adopt(toAssignTasks.stream().map(TaskTrigger::getTask).collect(
            Collectors.toList()), TaskStatus.ASSIGNING);

    }

    private void taskStatusChange(List<Task> reportedTasks) {
        Map<TaskStatus,List<Task>> taskUpdateMap = new HashMap<>();
        reportedTasks.forEach(task->{
            final List<Task> tasks = taskUpdateMap
                .computeIfAbsent(task.getStatus(), status -> new LinkedList<>());
            tasks.add(task);
        });
        taskUpdateMap.forEach((targetStatus,tasks)-> livingTaskStatusMachine
            .adopt(tasks,targetStatus));

    }

    List<TaskCommand> buildTaskCommands(List<TaskTrigger> toAssignTasks,Collection<Task> toCancelTasks){
        final Stream<TaskCommand> TaskTriggerStream = toAssignTasks.stream()
            .map(tk -> new TaskCommand(CommandType.TRIGGER, tk.getTask()));
        final Stream<TaskCommand> taskCancelStream = toCancelTasks.stream()
            .map(ct -> new TaskCommand(CommandType.CANCEL, ct));
        return Stream.concat(TaskTriggerStream,taskCancelStream).collect(Collectors.toList());
    }

    ReportResponse rebuildReportResponse(List<TaskCommand> TaskTriggers){
        List<Long> taskIdsToCancel = new LinkedList<>();

        List<TaskTrigger> tasksToRun = new LinkedList<>();

        TaskTriggers.forEach(taskCommand -> {
            if(taskCommand.getCommandType() == CommandType.CANCEL){
                taskIdsToCancel.add(taskCommand.getTask().getId());
            }else{
                tasksToRun.add(buildTaskTriggerFromTask(taskCommand.getTask()));
            }
        });

        return new ReportResponse(taskIdsToCancel,tasksToRun);

    }

    TaskTrigger buildTaskTriggerFromTask(Task task){
         //todo(renyanda) find swmp & swds
         return TaskTrigger.builder().task(task).build();
    }


}
