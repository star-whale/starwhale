/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.domain.task.TaskTrigger;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import lombok.Data;

public class TaskPool {

    public static final List<TaskTrigger> newTasks = new Vector<>();
    public static final Queue<AgentTask> preparingTasks = new ArrayDeque<>(4);
    public static final List<AgentTask> runningTasks = new Vector<>();
    public static final Queue<AgentTask> resultingTasks = new ArrayDeque<>(4);
    public static final Queue<AgentTask> finishedTasks = new ArrayDeque<>(4);
    public static final Queue<AgentTask> archivedTasks = new ArrayDeque<>(4);


    /**
     * whether init successfully
     */
    private static volatile boolean ready = false;

    public static boolean isReady() {
        return ready;
    }

    public void setToReady() {
        this.ready = true;
    }
}
