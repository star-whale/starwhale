/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.executor;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.PPLTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.agent.task.action.SelectOneToExecute;
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
    DoTransition<Void, List<PPLTask>> rebuildTasksAction;

    final
    DoTransition<PPLTask, PPLTask> init2PreparingAction;

    final
    DoTransition<PPLTask, PPLTask> preparing2RunningAction;

    final
    DoTransition<PPLTask, PPLTask> preparing2CanceledAction;

    final
    DoTransition<PPLTask, PPLTask> monitorRunningTaskAction;

    final
    DoTransition<PPLTask, PPLTask> running2CanceledAction;

    final
    DoTransition<PPLTask, PPLTask> uploading2FinishedAction;

    final
    DoTransition<PPLTask, PPLTask> uploading2CanceledAction;

    final
    DoTransition<ReportRequest, ReportResponse> reportAction;

    final
    DoTransition<PPLTask, PPLTask> finishedOrCanceled2ArchivedAction;


    /**
     * multiple choices operator
     */
    SelectOneToExecute<PPLTask, PPLTask> execute = new SelectOneToExecute<>() {
    };

    public TaskExecutor(
            SourcePool sourcePool,
            TaskPool taskPool,
            DoTransition<Void, List<PPLTask>> rebuildTasksAction,
            DoTransition<PPLTask, PPLTask> init2PreparingAction,
            DoTransition<PPLTask, PPLTask> preparing2RunningAction,
            DoTransition<PPLTask, PPLTask> preparing2CanceledAction,
            DoTransition<PPLTask, PPLTask> finishedOrCanceled2ArchivedAction,
            DoTransition<PPLTask, PPLTask> monitorRunningTaskAction,
            DoTransition<PPLTask, PPLTask> running2CanceledAction,
            DoTransition<PPLTask, PPLTask> uploading2FinishedAction,
            DoTransition<PPLTask, PPLTask> uploading2CanceledAction,
            DoTransition<ReportRequest, ReportResponse> reportAction) {
        this.rebuildTasksAction = rebuildTasksAction;
        this.sourcePool = sourcePool;
        this.taskPool = taskPool;
        this.init2PreparingAction = init2PreparingAction;
        this.preparing2RunningAction = preparing2RunningAction;
        this.preparing2CanceledAction = preparing2CanceledAction;
        this.finishedOrCanceled2ArchivedAction = finishedOrCanceled2ArchivedAction;
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
            execute.apply(
                    taskPool.preparingTasks.peek(),
                    Context.instance(),
                    task -> !taskPool.needToCancel.contains(task.getId()),
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
            for (PPLTask runningTask : new ArrayList<>(taskPool.runningTasks)) {
                // could be async
                execute.apply(
                        runningTask,
                        Context.instance(),
                        task -> !taskPool.needToCancel.contains(task.getId()),
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
            for (PPLTask uploadingTask : new ArrayList<>(taskPool.uploadingTasks)) {
                execute.apply(uploadingTask,
                        Context.instance(),
                        task -> !taskPool.needToCancel.contains(task.getId()),
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

