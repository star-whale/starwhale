/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

public class TaskPool {

    private Queue<AgentTask> preparingTasks = new ArrayDeque<>(4);
    private List<AgentTask> runningTasks = new Vector<>();
    private Queue<AgentTask> resultingTasks = new ArrayDeque<>(4);
    private Queue<AgentTask> finishedTasks = new ArrayDeque<>(4);
    private Queue<AgentTask> archivedTasks = new ArrayDeque<>(4);

    private List<String> needToCancel = new Vector<>();

    /**
     * 是否初始化完成
     */
    private volatile boolean canRun = false;
}
