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

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.system.agent.AgentCache;
import ai.starwhale.mlops.domain.task.cache.LivingTaskCache;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    final AgentCache agentCache;

    final ObjectMapper jsonMapper;

    final TaskMapper taskMapper;

    public ReportProcessorImp(CommandingTasksChecker commandingTasksChecker,
        @Qualifier("cacheWrapperForWatch") LivingTaskCache livingTaskCache, SWTaskScheduler swTaskScheduler,
        TaskBoConverter taskBoConverter,
        AgentCache agentCache, ObjectMapper jsonMapper,
        TaskMapper taskMapper) {
        this.commandingTasksChecker = commandingTasksChecker;
        this.livingTaskCache = livingTaskCache;
        this.swTaskScheduler = swTaskScheduler;
        this.taskBoConverter = taskBoConverter;
        this.agentCache = agentCache;
        this.jsonMapper = jsonMapper;
        this.taskMapper = taskMapper;
    }

    // 0.if node doesn't exists creat one 1. check commanding tasks;  2. change task status; 3. schedule task & cancel task;
    public ReportResponse receive(ReportRequest report) {
        final Node nodeInfo = report.getNodeInfo();
        if(null == nodeInfo){
            log.error("node info reported is null");
            return new ReportResponse(new ArrayList<>(),new ArrayList<>(), new ArrayList<>());
        }
        Agent agent = agentCache.nodeReport(nodeInfo);
        final List<TaskReport> taskReports = report.getTasks() == null? new ArrayList<>():report.getTasks();
        final List<ReportedTask> reportedTasks = taskReports.parallelStream().map(ReportedTask::from).collect(Collectors.toList());
        final List<TaskCommand> unProperTasks = commandingTasksChecker
            .onNodeReporting(agent, reportedTasks);
        Set<Long> unProperTaskIds = unProperTasks.parallelStream()
            .map(taskCommand -> taskCommand.getTask().getId()).collect(Collectors.toSet());
        copyTaskStatusFromAgentReport(reportedTasks.parallelStream().filter(rt->!unProperTaskIds.contains(rt.getId())).collect(
            Collectors.toList()));
        if (!CollectionUtils.isEmpty(unProperTasks)) {
            return rebuildReportResponse(unProperTasks);
        }
        final List<Task> toAssignTasks = swTaskScheduler.schedule(nodeInfo);
        scheduledTaskStatusChange(toAssignTasks,agent);
        commandingTasksChecker.onTaskCommanding(taskBoConverter.toTaskCommand(toAssignTasks),
            agent);

        final Collection<Task> toCancelTasks = livingTaskCache
            .ofStatus(TaskStatus.TO_CANCEL)
            .stream()
            .filter(t->t.getAgent().equals(agent))
            .collect(Collectors.toList());
        toCancelTaskStatusChange(toCancelTasks);
        commandingTasksChecker.onTaskCommanding(taskBoConverter.toTaskCommand(List.copyOf(toCancelTasks)),
            agent);
        return buidResponse(toAssignTasks, toCancelTasks);
    }

    public ReportResponse buidResponse(List<Task> toAssignTasks,
        Collection<Task> toCancelTasks) {
        final List<Long> taskIdsToCancel = toCancelTasks.stream().map(Task::getId)
            .collect(
                Collectors.toList());
        return new ReportResponse(
            taskIdsToCancel, taskBoConverter.toTaskTrigger(toAssignTasks), new ArrayList<>());
    }

    private void toCancelTaskStatusChange(Collection<Task> tasks) {
        tasks.forEach(task -> task.setStatus(TaskStatus.CANCELLING));
    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;
    private void scheduledTaskStatusChange(List<Task> toAssignTasks,Agent agent) {
        if(CollectionUtils.isEmpty(toAssignTasks)){
            return;
        }
        List<Long> taskIds = toAssignTasks.parallelStream().map(Task::getId).collect(
            Collectors.toList());
        BatchOperateHelper.doBatch(taskIds, agent.getId(),
            (tsks, agentid) -> taskMapper.updateTaskAgent(List.copyOf(tsks), agentid), MAX_BATCH_SIZE);
        toAssignTasks.stream().forEach(task -> {
            task.setAgent(agent);
            task.setStatus(TaskStatus.ASSIGNING);
        });
    }

    private void copyTaskStatusFromAgentReport(List<ReportedTask> reportedTasks) {

        reportedTasks.forEach(reportedTask -> {
            Collection<Task> optionalTasks = livingTaskCache.ofIds(List.of(reportedTask.getId()));
            if(null == optionalTasks || optionalTasks.isEmpty()){
                log.error("unknown optionalTasks reported {}",reportedTask.getId());
                return;
            }
            optionalTasks.forEach(task -> task.setStatus(reportedTask.getStatus()));
        });
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

        return new ReportResponse(taskIdsToCancel, tasksToRun, new ArrayList<>());
    }

}
