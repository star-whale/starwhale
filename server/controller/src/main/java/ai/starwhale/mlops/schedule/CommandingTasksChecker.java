/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.Agent;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
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

    final Map<Agent, Set<TaskCommand>> commandingTasks ;

    public CommandingTasksChecker(){
        commandingTasks = new ConcurrentHashMap<>();
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
        commandingTasks.computeIfAbsent(agent,n->Collections.synchronizedSet(new HashSet<>())).addAll(tasks);
    }

    /**
     * if the commanding task is not present properly in the node, then command the tasks again
     * else return empty
     * @param node reporting node
     * @return not properly present tasks that are commanding to this node.
     */
    public List<TaskCommand> onNodeReporting(Node node,List<Task> reportedTasks){
        final Set<TaskCommand> taskCommands = commandingTasks.get(Agent.fromNode(node));
        final Map<Long, Task> nodeTasks = reportedTasks.stream()
            .collect(Collectors.toMap(Task::getId,
                Function.identity()));
        final List<TaskCommand> unProperTasks = new LinkedList<>();
        final List<TaskCommand> properTasks = new LinkedList<>();
        taskCommands.forEach(taskCommand -> {
            final Task nodeTask = nodeTasks.get(taskCommand.getTask().getId());
            final boolean unproperlyExed = !taskCommand.agentProper(nodeTask);
            if(unproperlyExed){
                unProperTasks.add(taskCommand);
            }else {
                properTasks.add(taskCommand);
            }
        });

        taskCommands.removeAll(properTasks);

        return unProperTasks;
    }

}
