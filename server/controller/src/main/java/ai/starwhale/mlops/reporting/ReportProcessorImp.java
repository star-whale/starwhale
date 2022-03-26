/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportResponse;
import ai.starwhale.mlops.domain.node.DeviceHolder;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskCommand;
import ai.starwhale.mlops.domain.task.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.EvaluationTask;
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

    TaskStatusMachine taskStatusMachine;

    TaskScheduler taskScheduler;

    // 1. check commanding tasks; 2. change task status; 3. schedule task & cancel task;
     public ReportResponse receive(ReportRequest report){
         final Node nodeInfo = report.getNodeInfo();
         final List<TaskCommand> unProperTasks = commandingTasksChecker
             .onNodeReporting(nodeInfo);
         if(!CollectionUtils.isEmpty(unProperTasks)){
             return rebuildReportResponse(unProperTasks);
         }
         taskStatusChange(nodeInfo);
         final List<EvaluationTask> toAssignTasks = taskScheduler.schedule(nodeInfo);
         final Collection<Task> toCancelTasks = taskStatusMachine.ofStatus(TaskStatus.TO_CANCEL);
         scheduledTaskStatusChange(toAssignTasks);
         canceledTaskStatusChange(toCancelTasks);
         commandingTasksChecker.onTaskCommanding(buildTaskCommands(toAssignTasks,toCancelTasks),nodeInfo);
         return buidResponse(toAssignTasks, toCancelTasks);
     }

    public ReportResponse buidResponse(List<EvaluationTask> toAssignTasks,
        Collection<Task> toCancelTasks) {
        final List<String> taskIdsToCancel = toCancelTasks.stream().map(task -> task.getId().toString())
            .collect(
                Collectors.toList());
        return new ReportResponse(
            taskIdsToCancel, toAssignTasks);
    }

    private void canceledTaskStatusChange(Collection<Task> tasks) {
        taskStatusMachine.statusChange(tasks,TaskStatus.CANCEL_COMMANDING);
    }

    private void scheduledTaskStatusChange(List<EvaluationTask> toAssignTasks) {
        taskStatusMachine.statusChange(toAssignTasks.stream().map(EvaluationTask::getTask).collect(
            Collectors.toList()), TaskStatus.ASSIGNING);

    }

    private void taskStatusChange(Node nodeInfo) {
        Map<TaskStatus,List<Task>> taskUpdateMap = new HashMap<>();
        nodeInfo.getDeviceHolders().stream()
            .map(DeviceHolder::getHolder).forEach(task->{
            final List<Task> tasks = taskUpdateMap
                .computeIfAbsent(task.getStatus(), status -> new LinkedList<>());
            tasks.add(task);
        });
        taskUpdateMap.forEach((targetStatus,tasks)-> taskStatusMachine.statusChange(tasks,targetStatus));

    }

    List<TaskCommand> buildTaskCommands(List<EvaluationTask> toAssignTasks,Collection<Task> toCancelTasks){
        final Stream<TaskCommand> evaluationTaskStream = toAssignTasks.stream()
            .map(tk -> new TaskCommand(CommandType.TRIGGER, tk.getTask()));
        final Stream<TaskCommand> taskCancelStream = toCancelTasks.stream()
            .map(ct -> new TaskCommand(CommandType.CANCEL, ct));
        return Stream.concat(evaluationTaskStream,taskCancelStream).collect(Collectors.toList());
    }

    ReportResponse rebuildReportResponse(List<TaskCommand> evaluationTasks){
        List<String> taskIdsToCancel = new LinkedList<>();

        List<EvaluationTask> tasksToRun = new LinkedList<>();

        evaluationTasks.forEach(taskCommand -> {
            if(taskCommand.getCommandType() == CommandType.CANCEL){
                taskIdsToCancel.add(taskCommand.getTask().getId().toString());
            }else{
                tasksToRun.add(buildEvaluationTaskFromTask(taskCommand.getTask()));
            }
        });

        return new ReportResponse(taskIdsToCancel,tasksToRun);

    }

    EvaluationTask buildEvaluationTaskFromTask(Task task){
         //TODO find swmp & swds
         return EvaluationTask.builder().task(task).build();
    }


}
