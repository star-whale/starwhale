/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.Agent;
import ai.starwhale.mlops.domain.system.AgentEntity;
import ai.starwhale.mlops.domain.system.AgentMapper;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.TaskScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * the processor for every report from Agent
 */
@Slf4j
public class ReportProcessorImp implements ReportProcessor{

    CommandingTasksChecker commandingTasksChecker;

    LivingTaskStatusMachine livingTaskStatusMachine;

    TaskScheduler taskScheduler;

    TaskBoConverter taskBoConverter;

    AgentMapper agentMapper;

    ObjectMapper jsonMapper;

    // 0.if node doesn't exists creat one 1. check commanding tasks;  2. change task status; 3. schedule task & cancel task;
    @Transactional
     public ReportResponse receive(ReportRequest report){
         final Node nodeInfo = report.getNodeInfo();
        final AgentEntity agentEntity = agentMapper.findByIpForUpdate(nodeInfo.getIpAddr());
        if(null == agentEntity){
            insertAgent(nodeInfo);
        }
        final List<TaskReport> taskReports = report.getTasks();
         final List<Task> tasks = taskReports.parallelStream().map(taskReport -> {
             final Long taskId = taskReport.getId();
             final Task tsk = livingTaskStatusMachine.ofId(taskId)
                 .orElseGet(() -> {
                     log.warn("not hot task load into mem {}",taskId);
                     final Task task = taskBoConverter.fromId(taskId);
                     livingTaskStatusMachine.adopt(List.of(task),task.getStatus());
                     return task;
                 });
             tsk.setStatus(taskReport.getStatus());
             return tsk;
         }).collect(Collectors.toList());
         final List<TaskCommand> unProperTasks = commandingTasksChecker
             .onNodeReporting(nodeInfo,tasks);
         if(!CollectionUtils.isEmpty(unProperTasks)){
             return rebuildReportResponse(unProperTasks);
         }
         taskStatusChange(tasks);
         final List<Task> toAssignTasks = taskScheduler.schedule(nodeInfo);
         final Collection<Task> toCancelTasks = livingTaskStatusMachine.ofStatus(TaskStatus.TO_CANCEL);
         scheduledTaskStatusChange(toAssignTasks);
         canceledTaskStatusChange(toCancelTasks);
         commandingTasksChecker.onTaskCommanding(taskBoConverter.toTaskCommand(toAssignTasks),
             Agent.fromNode(nodeInfo));
         return buidResponse(toAssignTasks, toCancelTasks);
     }

    private void insertAgent(Node nodeInfo) {
        String deviceInfo="";
        try {
            deviceInfo = jsonMapper.writeValueAsString(nodeInfo.getDevices());
        } catch (JsonProcessingException e) {
            log.error("serialize device info from node failed",e);
        }
        final AgentEntity agentEntity = AgentEntity.builder()
            .agentIp(nodeInfo.getIpAddr())
            .agentVersion(nodeInfo.getAgentVersion())
            .connectTime(LocalDateTime.now())
            .deviceInfo(deviceInfo)
            .build();
        agentMapper.addAgent(agentEntity);
    }

    public ReportResponse buidResponse(List<Task> toAssignTasks,
        Collection<Task> toCancelTasks) {
        final List<Long> taskIdsToCancel = toCancelTasks.stream().map(Task::getId)
            .collect(
                Collectors.toList());
        return new ReportResponse(
            taskIdsToCancel, taskBoConverter.toTaskTrigger(toAssignTasks));
    }

    private void canceledTaskStatusChange(Collection<Task> tasks) {
        livingTaskStatusMachine.adopt(tasks,TaskStatus.CANCEL_COMMANDING);
    }

    private void scheduledTaskStatusChange(List<Task> toAssignTasks) {
        livingTaskStatusMachine.adopt(toAssignTasks, TaskStatus.ASSIGNING);

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

    ReportResponse rebuildReportResponse(List<TaskCommand> taskCommands){


        List<Long> taskIdsToCancel = taskCommands.parallelStream()
            .filter(taskCommand -> taskCommand.getCommandType() == CommandType.CANCEL)
            .map(TaskCommand::getTask)
            .map(Task::getId).collect(Collectors.toList());

        List<TaskTrigger> tasksToRun = taskCommands.parallelStream()
            .filter(taskCommand -> taskCommand.getCommandType() == CommandType.TRIGGER)
            .map(TaskCommand::getTask)
            .map(task -> taskBoConverter.toTaskTrigger(task)).collect(Collectors.toList());

        return new ReportResponse(taskIdsToCancel,tasksToRun);

    }

}
