/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction.Context;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction.SelectOneToExecute;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskPool;
import ai.starwhale.mlops.api.ReportApi;
import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * created -> preparing -> running -> resulting -> finished -> archived
 * -----\----------\-----------\----> error
 * ---------------------------------> canceled
 */
@Slf4j
public class TaskExecutor {

    private final AgentProperties agentProperties;

    private final ReportApi reportApi;

    private final ContainerClient containerClient;

    private final SourcePool sourcePool;

    private final TaskPool taskPool;

    public TaskExecutor(AgentProperties agentProperties,
        ReportApi reportApi,
        ContainerClient containerClient, SourcePool sourcePool,
        TaskPool taskPool) {
        this.agentProperties = agentProperties;
        this.reportApi = reportApi;
        this.containerClient = containerClient;
        this.sourcePool = sourcePool;
        this.taskPool = taskPool;
    }

    /**
     * multiple choices operator
     */
    SelectOneToExecute<EvaluationTask, EvaluationTask> execute = new SelectOneToExecute<>() {};

    /**
     * allocate device(GPU or CPU) for task
     */
    /*public void allocateDeviceForPreparingTasks() {
        if (sourcePool.isReady() && taskPool.isReady() && !taskPool.newTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            execute.apply(
                taskPool.newTasks.peek(),
                TaskAction.Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties),
                task -> !taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.init2Preparing,
                TaskAction.init2Canceled
            );
        }
    }*/

    /**
     * start container for preparing task
     */
    public void dealPreparingTasks() {
        if (sourcePool.isReady() && taskPool.isReady() && !taskPool.preparingTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            execute.apply(
                taskPool.preparingTasks.peek(),
                TaskAction.Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties),
                task -> !taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.preparing2Running,
                TaskAction.preparing2Canceled
            );
        }
    }

    /**
     * monitor the status of running task
     */
    public void monitorRunningTasks() {
        if (taskPool.isReady() && !taskPool.runningTasks.isEmpty()) {
            for (EvaluationTask runningTask : taskPool.runningTasks) {
                execute.apply(
                    runningTask,
                    TaskAction.Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties),
                    task -> !taskPool.needToCancel.contains(task.getTask().getId()),
                    TaskAction.running2Uploading,
                    TaskAction.running2Canceled
                );
            }

        }
    }

    /**
     * do upload
     */
    public void uploadResultingTasks() {
        if (taskPool.isReady() && !taskPool.uploadingTasks.isEmpty()) {
            execute.apply(taskPool.uploadingTasks.get(0),
                TaskAction.Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties),
                task -> !taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.uploading2Finished, TaskAction.uploading2Canceled);
        }
    }

    /**
     * report sync lock
     */
    private final AtomicBoolean reportMutex = new AtomicBoolean(false);

    /**
     * report info
     */
    public void reportTasks() {
        if (sourcePool.isReady() && taskPool.isReady() && reportMutex.compareAndSet(false, true)) {
            try {
                // all tasks(exclude archived) should be report to the controller
                ReportRequest request = ReportRequest.builder().build();

                Context context = TaskAction.Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties);
                // report it to controller
                TaskAction.report.apply(request, context);
            } catch (Exception e){
                log.error("report error:{}", e.getMessage());
            } finally {
                // release sync lock
                reportMutex.compareAndSet(true, false);
            }
        }
    }
}

