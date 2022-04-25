/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.executor;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.SelectOneToExecute;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * created -> preparing -> running -> resulting -> finished -> archived
 * -----\----------\-----------\----> error
 * ---------------------------------> canceled
 */
@Slf4j
public class TaskExecutor {

    final SourcePool sourcePool;

    final TaskPool taskPool;

    /* task actions start */
    final
    Action<Void, List<InferenceTask>> rebuildTasksAction;

    final
    Action<InferenceTask, InferenceTask> init2PreparingAction;

    final
    Action<InferenceTask, InferenceTask> preparing2RunningAction;

    final
    Action<InferenceTask, InferenceTask> preparing2CanceledAction;

    final
    Action<InferenceTask, InferenceTask> monitorRunningTaskAction;

    final
    Action<InferenceTask, InferenceTask> running2CanceledAction;

    final
    Action<InferenceTask, InferenceTask> uploading2FinishedAction;

    final
    Action<InferenceTask, InferenceTask> uploading2CanceledAction;

    final
    Action<ReportRequest, ReportResponse> reportAction;

    final
    Action<InferenceTask, InferenceTask> archivedAction;


    /**
     * multiple choices operator
     */
    SelectOneToExecute<InferenceTask, InferenceTask> execute = new SelectOneToExecute<>() {
    };

    public TaskExecutor(
            SourcePool sourcePool,
            TaskPool taskPool,
            Action<Void, List<InferenceTask>> rebuildTasksAction,
            Action<InferenceTask, InferenceTask> init2PreparingAction,
            Action<InferenceTask, InferenceTask> preparing2RunningAction,
            Action<InferenceTask, InferenceTask> preparing2CanceledAction,
            Action<InferenceTask, InferenceTask> archivedAction,
            Action<InferenceTask, InferenceTask> monitorRunningTaskAction,
            Action<InferenceTask, InferenceTask> running2CanceledAction,
            Action<InferenceTask, InferenceTask> uploading2FinishedAction,
            Action<InferenceTask, InferenceTask> uploading2CanceledAction,
            Action<ReportRequest, ReportResponse> reportAction) {
        this.rebuildTasksAction = rebuildTasksAction;
        this.sourcePool = sourcePool;
        this.taskPool = taskPool;
        this.init2PreparingAction = init2PreparingAction;
        this.preparing2RunningAction = preparing2RunningAction;
        this.preparing2CanceledAction = preparing2CanceledAction;
        this.archivedAction = archivedAction;
        this.monitorRunningTaskAction = monitorRunningTaskAction;
        this.running2CanceledAction = running2CanceledAction;
        this.uploading2FinishedAction = uploading2FinishedAction;
        this.uploading2CanceledAction = uploading2CanceledAction;
        this.reportAction = reportAction;
    }

    /**
     * start container for preparing task
     */
    public void dealPreparingTasks() {
        if (sourcePool.isReady() && taskPool.isReady() && !taskPool.preparingTasks.isEmpty()) {
            // deal with the preparing task with FIFO sort
            // todo whether async more fit
            execute.apply(
                    taskPool.preparingTasks.peek(),
                    Context.instance(),
                    task -> !taskPool.needToCancel.contains(task.getId()) && task.getStatus() != InferenceTaskStatus.CANCELING,
                    preparing2RunningAction,
                    preparing2CanceledAction
            );
        }
    }

    /**
     * monitor the status of running task
     */
    public void monitorRunningTasks() {
        if (taskPool.isReady() && !taskPool.runningTasks.isEmpty()) {
            for (InferenceTask runningTask : new ArrayList<>(taskPool.runningTasks)) {
                // could be async
                execute.apply(
                        runningTask,
                        Context.instance(),
                        task -> !taskPool.needToCancel.contains(task.getId()) && task.getStatus() != InferenceTaskStatus.CANCELING,
                        monitorRunningTaskAction,
                        running2CanceledAction
                );
            }
        }
    }

    /**
     * do upload
     */
    public void uploadTaskResults() {
        if (taskPool.isReady() && !taskPool.uploadingTasks.isEmpty()) {
            for (InferenceTask uploadingTask : new ArrayList<>(taskPool.uploadingTasks)) {
                execute.apply(uploadingTask,
                        Context.instance(),
                        task -> !taskPool.needToCancel.contains(task.getId()) && task.getStatus() != InferenceTaskStatus.CANCELING,
                        uploading2FinishedAction,
                        uploading2CanceledAction);

            }

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

                Context context = Context.instance();
                // report it to controller
                reportAction.apply(request, context);
            } catch (Exception e) {
                log.error("report error:{}", e.getMessage());
            } finally {
                // release sync lock
                reportMutex.compareAndSet(true, false);
            }
        }
    }
}

