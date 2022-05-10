/**
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

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.system.agent.AgentStatus;
import ai.starwhale.mlops.domain.system.agent.AgentStatusWatcher;
import ai.starwhale.mlops.domain.task.LivingTaskCache;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.reporting.ReportedTask;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * deal with the bad case when agent failed between Agent Reporting and Tasks commanding
 * ------controller----
 *   ↑               ↓
 * report            failed
 * --------agent-------
 */
@Slf4j
@Service
public class CommandingTasksChecker implements AgentStatusWatcher {

    final TaskStatusMachine taskStatusMachine;
    final Map<Agent, Set<TaskCommand>> commandingTaskAgentMap;
    final LivingTaskCache livingTaskCache;
    final SWTaskScheduler swTaskScheduler;

    public CommandingTasksChecker(
        TaskStatusMachine taskStatusMachine,
        LivingTaskCache livingTaskCache,
        SWTaskScheduler swTaskScheduler){
        this.taskStatusMachine = taskStatusMachine;
        this.livingTaskCache = livingTaskCache;
        this.swTaskScheduler = swTaskScheduler;
        commandingTaskAgentMap = new ConcurrentHashMap<>();
    }

    /**
     * whenever a command is dispatched to Agent, this method is expected to be called
     * @param tasks the task command to Agent
     * @param agent the agent info of the Agent
     */
    public void onTaskCommanding(List<TaskCommand> tasks,Agent agent){
        if(CollectionUtils.isEmpty(tasks)){
            return;
        }
        commandingTaskAgentMap.computeIfAbsent(agent,n->Collections.synchronizedSet(new HashSet<>())).addAll(tasks);
    }

    /**
     * if the commanding task is not present properly in the agent, then command the tasks again
     * else return empty
     * @param agent reporting agent
     * @return not properly present tasks that are commanding to this agent.
     */
    public List<TaskCommand> onNodeReporting(Agent agent,List<ReportedTask> reportedTasks){
        final Set<TaskCommand> commandingTasks = commandingTaskAgentMap.computeIfAbsent(agent,k->Collections.synchronizedSet(new HashSet<>()));
        final Map<Long, ReportedTask> reportedTaskMap = reportedTasks.stream()
            .collect(Collectors.toMap(ReportedTask::getId,
                Function.identity()));
        final List<TaskCommand> unProperTasks = new LinkedList<>();
        final List<TaskCommand> properTasks = new LinkedList<>();
        commandingTasks.forEach(taskCommand -> {
            Long taskId = taskCommand.getTask().getId();
            final ReportedTask rt = reportedTaskMap.get(taskId);
            final boolean unproperlyExed = null == rt || !taskStatusMachine.couldTransfer(taskCommand.getCommandType().getCorrespondStatus(),rt.getStatus());
            if(unproperlyExed){
                log.warn("unproper task found {}",taskId);
                unProperTasks.add(taskCommand);
            }else {
                properTasks.add(taskCommand);
            }
        });

        commandingTasks.removeAll(properTasks);

        return unProperTasks;
    }

    @Override
    public void agentStatusChange(Agent agent, AgentStatus newStatus) {
        log.debug("agent status change to {} watched by {}",newStatus,this.getClass().getName());
        if(AgentStatus.OFFLINE != newStatus){
            return;
        }
        Set<TaskCommand> taskCommands = commandingTaskAgentMap.get(agent);
        commandingTaskAgentMap.remove(agent);
        if(null == taskCommands || taskCommands.isEmpty()){
            return;
        }
        reScheduleTasks(taskCommands);

    }

    private void reScheduleTasks(Set<TaskCommand> taskCommands) {
        List<Long> taskIds = taskCommands.parallelStream().map(tc -> tc.getTask().getId())
            .collect(Collectors.toList());
        livingTaskCache.update(taskIds, TaskStatus.CREATED);
        Map<Clazz, List<Task>> deviceGroup = taskCommands.parallelStream().map(TaskCommand::getTask)
            .collect(Collectors.groupingBy(task -> task.getJob().getJobRuntime().getDeviceClass()));
        deviceGroup.forEach((deviceClass,tasks)->{
            swTaskScheduler.adoptTasks(tasks,deviceClass);
        });
    }
}
