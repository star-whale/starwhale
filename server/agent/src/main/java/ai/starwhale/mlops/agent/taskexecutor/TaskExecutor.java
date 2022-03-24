/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction.Context;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction.SelectOneToExecute;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskPool;
import ai.starwhale.mlops.api.ReportApi;
import ai.starwhale.mlops.domain.task.EvaluationTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * created -> preparing -> running -> resulting -> finished -> archived
 * -----\----------\-----------\----> error
 * ---------------------------------> canceled
 */
@Slf4j
@Component
public class TaskExecutor {

    private final ContainerClient containerClient;

    private final ReportApi reportApi;

    private final AgentProperties agentProperties;

    private final SourcePool sourcePool;

    private final TaskPool taskPool;

    private final Context context;

    public TaskExecutor(
            ContainerClient containerClient, ReportApi reportApi,
            AgentProperties agentProperties, SourcePool sourcePool, TaskPool taskPool) {
        this.containerClient = containerClient;
        this.reportApi = reportApi;
        this.agentProperties = agentProperties;
        this.sourcePool = sourcePool;
        this.taskPool = taskPool;
        this.context = Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties);
    }

    SelectOneToExecute<EvaluationTask, EvaluationTask> execute = new SelectOneToExecute<>() {};

    /**
     * allocate device(GPU or CPU) for task
     */
    @Scheduled
    public void allocateDeviceForPreparingTasks() {
        while (sourcePool.isReady() && taskPool.isReady() && !taskPool.newTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            execute.apply(
                taskPool.newTasks.peek(),
                context,
                task -> taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.init2Preparing,
                TaskAction.init2Canceled
            );
        }
    }

    /**
     * start container for preparing task
     */
    @Scheduled
    public void dealPreparingTasks() {
        while (sourcePool.isReady() && taskPool.isReady() && !taskPool.preparingTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            TaskAction.preparing2Running.apply(taskPool.preparingTasks.peek(), context);
            execute.apply(
                taskPool.preparingTasks.peek(),
                context,
                task -> taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.preparing2Running,
                TaskAction.preparing2Canceled
            );
        }
    }

    /**
     * monitor the status of running task
     */
    @Scheduled
    public void monitorRunningTasks() {
        while (taskPool.isReady() && !taskPool.runningTasks.isEmpty()) {
            for (EvaluationTask runningTask : taskPool.runningTasks) {
                execute.apply(
                    runningTask,
                    context,
                    task -> taskPool.needToCancel.contains(task.getTask().getId()),
                    TaskAction.running2Uploading,
                    TaskAction.running2Canceled
                );
            }

        }
    }

    /**
     * do upload
     */
    @Scheduled
    public void uploadResultingTasks() {
        while (taskPool.isReady() && !taskPool.uploadingTasks.isEmpty()) {
            execute.apply(taskPool.uploadingTasks.get(0), context,
                task -> taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.uploading2Finished, TaskAction.uploading2Canceled);
        }
    }

    @Scheduled
    public void reportTasks() {
        if (sourcePool.isReady() && taskPool.isReady()) {
            // todo: all tasks should be report to the controller
            // when success,archived the finished task,and rm to the archive dir
        }
    }
}

