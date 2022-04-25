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
import ai.starwhale.mlops.domain.task.LivingTaskCache;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    final LivingTaskCache livingTaskCache;

    final SWTaskScheduler swTaskScheduler;

    final TaskBoConverter taskBoConverter;

    final AgentMapper agentMapper;

    final ObjectMapper jsonMapper;

    final TaskMapper taskMapper;

    public ReportProcessorImp(CommandingTasksChecker commandingTasksChecker,
        LivingTaskCache livingTaskCache, SWTaskScheduler swTaskScheduler,
        TaskBoConverter taskBoConverter, AgentMapper agentMapper, ObjectMapper jsonMapper,
        TaskMapper taskMapper) {
        this.commandingTasksChecker = commandingTasksChecker;
        this.livingTaskCache = livingTaskCache;
        this.swTaskScheduler = swTaskScheduler;
        this.taskBoConverter = taskBoConverter;
        this.agentMapper = agentMapper;
        this.jsonMapper = jsonMapper;
        this.taskMapper = taskMapper;
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
        final List<ReportedTask> reportedTasks = taskReports.parallelStream().map(ReportedTask::from).collect(Collectors.toList());
        final List<TaskCommand> unProperTasks = commandingTasksChecker
            .onNodeReporting(nodeInfo, reportedTasks);
        Set<Long> unProperTaskIds = unProperTasks.parallelStream()
            .map(taskCommand -> taskCommand.getTask().getId()).collect(Collectors.toSet());
        taskStatusChange(reportedTasks.parallelStream().filter(rt->!unProperTaskIds.contains(rt.getId())).collect(
            Collectors.toList()));
        if (!CollectionUtils.isEmpty(unProperTasks)) {
            return rebuildReportResponse(unProperTasks);
        }
        final List<Task> toAssignTasks = swTaskScheduler.schedule(nodeInfo);
        final Collection<Task> toCancelTasks = livingTaskCache
            .ofStatus(TaskStatus.TO_CANCEL)
            .stream()
            .filter(t->null != t.getAgent() && t.getAgent().equals(Agent.fromNode(nodeInfo)))
            .collect(Collectors.toList());
        scheduledTaskStatusChange(toAssignTasks,agentEntity);
        commandingTasksChecker.onTaskCommanding(taskBoConverter.toTaskCommand(toAssignTasks),
            Agent.fromNode(nodeInfo));
        canceledTaskStatusChange(toCancelTasks);
        commandingTasksChecker.onTaskCommanding(taskBoConverter.toTaskCommand(List.copyOf(toCancelTasks)),
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
        livingTaskCache.update(tasks.parallelStream().map(Task::getId).collect(Collectors.toList()), TaskStatus.CANCELLING);
    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;
    private void scheduledTaskStatusChange(List<Task> toAssignTasks,AgentEntity agentEntity) {
        if(CollectionUtils.isEmpty(toAssignTasks)){
            return;
        }
        List<Long> taskIds = toAssignTasks.parallelStream().map(Task::getId).collect(
            Collectors.toList());
        BatchOperateHelper.doBatch(taskIds, agentEntity.getId(),
            (tsks, agentid) -> taskMapper.updateTaskAgent(List.copyOf(tsks), agentid), MAX_BATCH_SIZE);
        toAssignTasks.parallelStream().forEach(task -> task.setAgent(Agent.fromEntity(agentEntity)));
        livingTaskCache.update(taskIds, TaskStatus.ASSIGNING);
    }

    private void taskStatusChange(List<ReportedTask> reportedTasks) {
        reportedTasks.parallelStream()
            .collect(Collectors.groupingBy(ReportedTask::getStatus))
            .forEach((targetStatus, tasks) -> livingTaskCache.update(tasks.parallelStream().map(ReportedTask::getId).collect(
                Collectors.toList()), targetStatus));
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
