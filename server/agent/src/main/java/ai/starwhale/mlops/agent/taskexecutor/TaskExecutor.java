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
import ai.starwhale.mlops.domain.task.TaskTrigger;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import org.springframework.scheduling.annotation.Scheduled;

public class TaskExecutor {

    private List<String> needToCancel = new Vector<>();

    private final ContainerClient containerClient;

    private final AgentProperties agentProperties;

    public TaskExecutor(
        ContainerClient containerClient, AgentProperties agentProperties) {
        this.containerClient = containerClient;
        this.agentProperties = agentProperties;
    }

    public void allocationDeviceForTask() {
        while (SourcePool.isReady() && TaskPool.isReady() && !TaskPool.newTasks.isEmpty()) {
            for (TaskTrigger taskTrigger : TaskPool.newTasks) {
                AgentTask agentTask = AgentTask.builder()
                    .id(taskTrigger.getTask().getId())
                    .jobId(taskTrigger.getTask().getJobId())
                    .status(TaskStatus.PREPARING)// now is preparing
                    .swModelPackage(taskTrigger.getSwModelPackage())
                    .swDataSetSlice(taskTrigger.getSwDataSetSlice())
                    .devices(SourcePool.allocate(1)) //
                    .build();
                // add the new task to the tail
                TaskPool.preparingTasks.offer(agentTask);
            }
        }
    }

    /**
     * blocking schedule
     */
    public void dealPreparingTasks() {
        while (SourcePool.isReady() && TaskPool.isReady() && !TaskPool.preparingTasks.isEmpty()) {
            AgentTask task;
            // deal with the preparing task with FIFO sort
            while ((task = TaskPool.preparingTasks.peek()) != null) {
                // todo fill with task info
                Optional<String> containerId = containerClient.startContainer("", ImageConfig.builder().build());
                // whether the container create and start success
                if (containerId.isPresent()) {
                    // remove it from head
                    task = TaskPool.preparingTasks.poll();
                    assert task != null;
                    task.setContainerId(containerId.get());
                    task.setStatus(TaskStatus.RUNNING);
                    // tail it to the running list
                    TaskPool.runningTasks.add(task);
                    try {
                        Path taskPath = Path.of(
                            agentProperties.getTask().getInfoPath() + "/" + task.getId());
                        if (!Files.exists(taskPath)) {
                            Files.createFile(taskPath);
                        }
                        // update info to the task file
                        Files.writeString(taskPath, JSONUtil.toJsonStr(task));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // todo: retry or take it to the tail of queue
                }
            }
        }
    }

    /**
     *
     */
    public void monitorRunningTasks() {
        while (TaskPool.isReady() && !TaskPool.runningTasks.isEmpty()) {
            TaskPool.runningTasks.forEach(runningTask -> {
                try {
                    // get the newest task info
                    Path taskPath = Path.of(
                        agentProperties.getTask().getInfoPath() + "/" + runningTask.getId());
                    String json = Files.readString(taskPath);
                    AgentTask newTask = JSONUtil.toBean(json, AgentTask.class);
                    // if run success
                    if (newTask.getStatus() == TaskStatus.RESULTING) {
                        // release device to available device pool todo:is there anything else to do?
                        SourcePool.free(newTask.getDevices());
                        // newTask.setDevices(null);
                        TaskPool.runningTasks.remove(runningTask);
                        TaskPool.resultingTasks.add(newTask);

                    } else if (newTask.getStatus() == TaskStatus.EXIT_ERROR) {
                        // todo
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     *
     */
    public void uploadResultingTasks() {
        while (TaskPool.isReady() && !TaskPool.resultingTasks.isEmpty()) {
            TaskPool.resultingTasks.forEach(resultingTask -> {
                Path taskPath = Path.of(
                    agentProperties.getTask().getInfoPath() + "/" + resultingTask.getId());
                // todo: upload result file to the storage
                resultingTask.setResultPaths(List.of(""));
                TaskPool.resultingTasks.remove(resultingTask);
                TaskPool.finishedTasks.add(resultingTask);
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
        if (SourcePool.isReady() && TaskPool.isReady()) {
            // all tasks should be report to the controller
            // when success,archived the finished task,and rm to the archive dir
        }
    }
}
