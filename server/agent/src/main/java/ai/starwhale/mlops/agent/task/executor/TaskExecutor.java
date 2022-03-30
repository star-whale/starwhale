/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.executor;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.agent.task.action.SelectOneToExecute;
import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * created -> preparing -> running -> resulting -> finished -> archived
 * -----\----------\-----------\----> error
 * ---------------------------------> canceled
 */
@Slf4j
@Service("agentTaskExecutor")
public class TaskExecutor {

    final SourcePool sourcePool;

    final TaskPool taskPool;

    /* task actions start */
    final
    DoTransition<String, List<EvaluationTask>> rebuildTasksAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> init2PreparingAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> preparing2RunningAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> preparing2CanceledAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> monitorRunningTaskAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> running2CanceledAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> uploading2FinishedAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> uploading2CanceledAction;

    final
    DoTransition<ReportRequest, ReportResponse> reportAction;

    final
    DoTransition<EvaluationTask, EvaluationTask> finishedOrCanceled2ArchivedAction;


    /**
     * multiple choices operator
     */
    SelectOneToExecute<EvaluationTask, EvaluationTask> execute = new SelectOneToExecute<>() {
    };

    public TaskExecutor(
            SourcePool sourcePool,
            TaskPool taskPool,
            DoTransition<String, List<EvaluationTask>> rebuildTasksAction,
            DoTransition<EvaluationTask, EvaluationTask> init2PreparingAction,
            DoTransition<EvaluationTask, EvaluationTask> preparing2RunningAction,
            DoTransition<EvaluationTask, EvaluationTask> preparing2CanceledAction,
            DoTransition<EvaluationTask, EvaluationTask> finishedOrCanceled2ArchivedAction,
            DoTransition<EvaluationTask, EvaluationTask> monitorRunningTaskAction,
            DoTransition<EvaluationTask, EvaluationTask> running2CanceledAction,
            DoTransition<EvaluationTask, EvaluationTask> uploading2FinishedAction,
            DoTransition<EvaluationTask, EvaluationTask> uploading2CanceledAction,
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
                    task -> !taskPool.needToCancel.contains(task.getTask().getId()),
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
            for (EvaluationTask runningTask : new ArrayList<>(taskPool.runningTasks)) {
                // could be async
                execute.apply(
                        runningTask,
                        Context.instance(),
                        task -> !taskPool.needToCancel.contains(task.getTask().getId()),
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
            for (EvaluationTask uploadingTask : new ArrayList<>(taskPool.uploadingTasks)) {
                execute.apply(uploadingTask,
                        Context.instance(),
                        task -> !taskPool.needToCancel.contains(task.getTask().getId()),
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

