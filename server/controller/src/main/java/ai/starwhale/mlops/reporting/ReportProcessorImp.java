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
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.Agent;
import ai.starwhale.mlops.domain.system.AgentEntity;
import ai.starwhale.mlops.domain.system.mapper.AgentMapper;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.task.status.StatusAdapter;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * the processor for every report from Agent
 */
@Slf4j
@Service
public class ReportProcessorImp implements ReportProcessor {

    final CommandingTasksChecker commandingTasksChecker;

    final LivingTaskStatusMachine livingTaskStatusMachine;

    final SWTaskScheduler swTaskScheduler;

    final TaskBoConverter taskBoConverter;

    final AgentMapper agentMapper;

    final ObjectMapper jsonMapper;

    final TaskMapper taskMapper;

    final StatusAdapter statusAdapter;

    public ReportProcessorImp(CommandingTasksChecker commandingTasksChecker,
        LivingTaskStatusMachine livingTaskStatusMachine, SWTaskScheduler swTaskScheduler,
        TaskBoConverter taskBoConverter, AgentMapper agentMapper, ObjectMapper jsonMapper,
        TaskMapper taskMapper, StatusAdapter statusAdapter) {
        this.commandingTasksChecker = commandingTasksChecker;
        this.livingTaskStatusMachine = livingTaskStatusMachine;
        this.swTaskScheduler = swTaskScheduler;
        this.taskBoConverter = taskBoConverter;
        this.agentMapper = agentMapper;
        this.jsonMapper = jsonMapper;
        this.taskMapper = taskMapper;
        this.statusAdapter = statusAdapter;
    }

    // 0.if node doesn't exists creat one 1. check commanding tasks;  2. change task status; 3. schedule task & cancel task;
    @Transactional
    public ReportResponse receive(ReportRequest report) {
        final Node nodeInfo = report.getNodeInfo();
        if(null == nodeInfo){
            log.error("node info reported is null");
            return new ReportResponse(new ArrayList<>(),new ArrayList<>());
        }
        AgentEntity agentEntity = agentMapper.findByIpForUpdate(nodeInfo.getIpAddr());
        if (null == agentEntity) {
            agentEntity = insertAgent(nodeInfo);
        }
        final List<TaskReport> taskReports = report.getTasks() == null? new ArrayList<>():report.getTasks();
        final List<Task> tasks = taskReports.parallelStream().map(taskReport -> {
            final Long taskId = taskReport.getId();
            final Task tsk = livingTaskStatusMachine.ofId(taskId)
                .orElseGet(() -> {
                    log.warn("not hot task load into mem {}", taskId);
                    final Task task = taskBoConverter.fromId(taskId);
                    livingTaskStatusMachine.adopt(List.of(task), task.getStatus());
                    return task;
                });
            tsk.setStatus(statusAdapter.from(taskReport.getStatus()));
            return tsk;
        }).collect(Collectors.toList());
        final List<TaskCommand> unProperTasks = commandingTasksChecker
            .onNodeReporting(nodeInfo, tasks);
        if (!CollectionUtils.isEmpty(unProperTasks)) {
            return rebuildReportResponse(unProperTasks);
        }
        taskStatusChange(tasks);
        final List<Task> toAssignTasks = swTaskScheduler.schedule(nodeInfo);
        final Collection<Task> toCancelTasks = livingTaskStatusMachine
            .ofStatus(TaskStatus.TO_CANCEL)
            .stream()
            .filter(t->t.getAgent().equals(Agent.fromNode(nodeInfo)))
            .collect(Collectors.toList());
        scheduledTaskStatusChange(toAssignTasks,agentEntity);
        canceledTaskStatusChange(toCancelTasks);
        commandingTasksChecker.onTaskCommanding(taskBoConverter.toTaskCommand(toAssignTasks),
            Agent.fromNode(nodeInfo));
        return buidResponse(toAssignTasks, toCancelTasks);
    }

    private AgentEntity insertAgent(Node nodeInfo) {
        String deviceInfo = "";
        try {
            deviceInfo = jsonMapper.writeValueAsString(nodeInfo.getDevices());
        } catch (JsonProcessingException e) {
            log.error("serialize device info from node failed", e);
        }
        final AgentEntity agentEntity = AgentEntity.builder()
            .agentIp(nodeInfo.getIpAddr())
            .agentVersion(nodeInfo.getAgentVersion())
            .connectTime(LocalDateTime.now())
            .deviceInfo(deviceInfo)
            .build();
        agentMapper.addAgent(agentEntity);
        return agentEntity;
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
        livingTaskStatusMachine.update(tasks, TaskStatus.CANCELLING);
    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;
    private void scheduledTaskStatusChange(List<Task> toAssignTasks,AgentEntity agentEntity) {
        if(CollectionUtils.isEmpty(toAssignTasks)){
            return;
        }
        BatchOperateHelper.doBatch(toAssignTasks, agentEntity.getId(),
            (tsks, agentid) -> taskMapper.updateTaskAgent(
                tsks.parallelStream().map(Task::getId).collect(
                    Collectors.toList()), agentid), MAX_BATCH_SIZE);
        toAssignTasks.parallelStream().forEach(task -> task.setAgent(Agent.fromEntity(agentEntity)));
        livingTaskStatusMachine.adopt(toAssignTasks, TaskStatus.ASSIGNING);
    }

    private void taskStatusChange(List<Task> reportedTasks) {
        reportedTasks.parallelStream()
            .collect(Collectors.groupingBy(Task::getStatus))
            .forEach((targetStatus, tasks) -> livingTaskStatusMachine.update(tasks, targetStatus));
    }

    ReportResponse rebuildReportResponse(List<TaskCommand> taskCommands) {
        List<Long> taskIdsToCancel = taskCommands.parallelStream()
            .filter(taskCommand -> taskCommand.getCommandType() == CommandType.CANCEL)
            .map(TaskCommand::getTask)
            .map(Task::getId).collect(Collectors.toList());

        List<TaskTrigger> tasksToRun = taskCommands.parallelStream()
            .filter(taskCommand -> taskCommand.getCommandType() == CommandType.TRIGGER)
            .map(TaskCommand::getTask)
            .map(task -> taskBoConverter.toTaskTrigger(task)).collect(Collectors.toList());

        return new ReportResponse(taskIdsToCancel, tasksToRun);
    }

}
