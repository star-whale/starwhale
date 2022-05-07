/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
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
public class CommandingTasksChecker {

    final TaskStatusMachine taskStatusMachine;
    final Map<Agent, Set<TaskCommand>> commandingTaskAgentMap;

    public CommandingTasksChecker(
        TaskStatusMachine taskStatusMachine){
        this.taskStatusMachine = taskStatusMachine;
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

}
