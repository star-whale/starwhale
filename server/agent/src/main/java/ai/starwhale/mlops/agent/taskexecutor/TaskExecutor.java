/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Vector;
import org.springframework.scheduling.annotation.Scheduled;

public class TaskExecutor {

    private Queue<AgentTask> preparingTasks = new ArrayDeque<>(4);
    private List<AgentTask> runningTasks = new Vector<>();
    private Queue<AgentTask> resultingTasks = new ArrayDeque<>(4);
    private Queue<AgentTask> finishedTasks = new ArrayDeque<>(4);
    private Queue<AgentTask> archivedTasks = new ArrayDeque<>(4);

    private List<String> needToCancel = new Vector<>();

    private final ContainerClient containerClient;

    public TaskExecutor(
        ContainerClient containerClient, AgentProperties agentProperties) {
        this.containerClient = containerClient;
        this.agentProperties = agentProperties;
    }

    private boolean valid() {
        // valid whether the source pool and the task pool init success
        return true;
    }
    /**
     * blocking schedule
     */
    @Scheduled()
    public void dealPreparingTasks() {
        if (valid()) {
            if (preparingTasks.isEmpty()) {
                return;
            }
            AgentTask task;
            while ((task = preparingTasks.peek()) != null) {
                Optional<String> containerId = containerClient.startContainer("",
                    ImageConfig.builder().build());
                if (containerId.isPresent()) {
                    // remove it from head
                    task = preparingTasks.poll();
                    assert task != null;
                    task.setContainerId(containerId.get());
                    task.setStatus(TaskStatus.RUNNING);
                    // tail it to the running list
                    runningTasks.add(task);
                    // todo: update info to the task file
                } else {
                    // todo: retry or take it to the tail of queue
                }
            }
        }
    }

    private final AgentProperties agentProperties;

    /**
     *
     */
    @Scheduled
    public void monitorRunningTasks() {
        if(valid()) {
            if(runningTasks.isEmpty()) return;
            runningTasks.forEach(runningTask -> {
                try {
                    Path taskPath = Path.of(agentProperties.getTask().getInfoPath() + "/" + runningTask.getId());
                    String json = Files.readString(taskPath);
                    AgentTask newTask = JSONUtil.toBean(json, AgentTask.class);
                    if(newTask.getStatus() == TaskStatus.RESULTING) {
                        // release device to available device pool todo:is there anything else to do?
                        SourcePool.free(newTask.getDevices());
                        // newTask.setDevices(null);
                        runningTasks.remove(runningTask);
                        resultingTasks.add(newTask);

                    } else if (newTask.getStatus() == TaskStatus.EXIT_ERROR) {
                        // todo
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }/**
     *
     */
    @Scheduled
    public void uploadResultingTasks() {
        if(valid()) {
            if(resultingTasks.isEmpty()) return;
            resultingTasks.forEach(resultingTask -> {
                Path taskPath = Path.of(agentProperties.getTask().getInfoPath() + "/" + resultingTask.getId());
                // todo: upload result file to the storage
                resultingTask.setResultPaths(List.of(""));
                resultingTasks.remove(resultingTask);
                finishedTasks.add(resultingTask);
                try {
                    // update info to the task file
                    Files.writeString(taskPath, JSONUtil.toJsonStr(resultingTask));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Scheduled
    public void reportTasks() {
        if(valid()) {
            // all tasks should be report to the controller
            // when success,archived the finished task,and rm to the archive dir
        }
    }
}
