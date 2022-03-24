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
 * created -> preparing -> running -> resulting -> finished -> archived \          \
 * \----> error ---------------------------> canceled
 */
@Slf4j
@Component
public class TaskExecutor {

    private final ContainerClient containerClient;

    private final ReportApi reportApi;

    private final AgentProperties agentProperties;

    public TaskExecutor(
        ContainerClient containerClient, ReportApi reportApi,
        AgentProperties agentProperties) {
        this.containerClient = containerClient;
        this.reportApi = reportApi;
        this.agentProperties = agentProperties;
    }

    SelectOneToExecute<EvaluationTask, EvaluationTask> execute = new SelectOneToExecute<>() {};

    public void allocateDeviceForPreparingTasks() {
        while (SourcePool.isReady() && TaskPool.isReady() && !TaskPool.newTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            Context context = Context.instance();
            context.set("taskInfoPath", agentProperties.getTask().getInfoPath());

            execute.apply(TaskPool.newTasks.peek(), context,
                task -> TaskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.init2Preparing, TaskAction.init2Canceled);
        }
    }

    /**
     * blocking schedule
     */
    public void dealPreparingTasks() {
        while (SourcePool.isReady() && TaskPool.isReady() && !TaskPool.preparingTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            Context context = Context.instance();
            context.set("taskInfoPath", agentProperties.getTask().getInfoPath());
            TaskAction.preparing2Running.apply(TaskPool.preparingTasks.peek(), context);
            execute.apply(TaskPool.preparingTasks.peek(), context,
                task -> TaskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.preparing2Running, TaskAction.preparing2Canceled);
        }
    }

    /**
     *
     */
    public void monitorRunningTasks() {
        while (TaskPool.isReady() && !TaskPool.runningTasks.isEmpty()) {
            Context context = Context.instance();
            context.set("taskInfoPath", agentProperties.getTask().getInfoPath());

            execute.apply(TaskPool.runningTasks.get(0), context,
                task -> TaskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.running2Uploading, TaskAction.running2Canceled);
        }
    }

    /**
     *
     */
    public void uploadResultingTasks() {
        while (TaskPool.isReady() && !TaskPool.resultingTasks.isEmpty()) {
            Context context = Context.instance();
            context.set("taskInfoPath", agentProperties.getTask().getInfoPath());
            context.set("containerClient", containerClient);

            execute.apply(TaskPool.resultingTasks.get(0), context,
                task -> TaskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.uploading2Finished, TaskAction.uploading2Canceled);
        }
    }

    @Scheduled
    public void reportTasks() {
        if (SourcePool.isReady() && TaskPool.isReady()) {
            // todo: all tasks should be report to the controller
            // when success,archived the finished task,and rm to the archive dir
        }
    }
}

