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
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportResponse;
import ai.starwhale.mlops.domain.task.EvaluationTask;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final ReportApi reportApi;

    private final SourcePool sourcePool;

    private final TaskPool taskPool;

    private final Context context;

    public TaskExecutor(
            ContainerClient containerClient, ReportApi reportApi,
            AgentProperties agentProperties, SourcePool sourcePool, TaskPool taskPool) {
        this.reportApi = reportApi;
        this.sourcePool = sourcePool;
        this.taskPool = taskPool;
        this.context = Context.instance(sourcePool, taskPool, reportApi, containerClient, agentProperties);
    }

    /**
     * multiple choices operator
     */
    SelectOneToExecute<EvaluationTask, EvaluationTask> execute = new SelectOneToExecute<>() {};

    /**
     * allocate device(GPU or CPU) for task
     */
    @Scheduled
    public void allocateDeviceForPreparingTasks() {
        if (sourcePool.isReady() && taskPool.isReady() && !taskPool.newTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            execute.apply(
                taskPool.newTasks.peek(),
                context,
                task -> !taskPool.needToCancel.contains(task.getTask().getId()),
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
        if (sourcePool.isReady() && taskPool.isReady() && !taskPool.preparingTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            TaskAction.preparing2Running.apply(taskPool.preparingTasks.peek(), context);
            execute.apply(
                taskPool.preparingTasks.peek(),
                context,
                task -> !taskPool.needToCancel.contains(task.getTask().getId()),
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
        if (taskPool.isReady() && !taskPool.runningTasks.isEmpty()) {
            for (EvaluationTask runningTask : taskPool.runningTasks) {
                execute.apply(
                    runningTask,
                    context,
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
    @Scheduled
    public void uploadResultingTasks() {
        if (taskPool.isReady() && !taskPool.uploadingTasks.isEmpty()) {
            execute.apply(taskPool.uploadingTasks.get(0), context,
                task -> !taskPool.needToCancel.contains(task.getTask().getId()),
                TaskAction.uploading2Finished, TaskAction.uploading2Canceled);
        }
    }

    /**
     * report sync lock
     */
    private final AtomicBoolean reportMutex = new AtomicBoolean(false);

    @Scheduled
    public void reportTasks() {
        if (sourcePool.isReady() && taskPool.isReady() && reportMutex.compareAndSet(false, true)) {
            try {
                // todo: all tasks(exclude archived) should be report to the controller
                // finished tasks should be snapshot(it means must to link current finished that, ensure ...), not only reference!!
                List<EvaluationTask> finishedTasks = List.copyOf(taskPool.finishedTasks);
                ReportRequest request = ReportRequest.builder().build();
                ResponseMessage<ReportResponse> response = reportApi.report(request);
                if (Objects.equals(response.getCode(), "success")) { // todo: when coding completed, change protocol:Code to sdk
                    // when success,archived the finished task,and rm to the archive dir
                    for (EvaluationTask finishedTask : finishedTasks) {
                        TaskAction.finished2Archived.apply(finishedTask, context);
                    }
                }
            } catch (Exception e){
                log.error("report error:{}", e.getMessage());
            } finally {
                // release sync lock
                reportMutex.compareAndSet(true, false);
            }
        }
    }
}

