/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask;

import ai.starwhale.mlops.agent.task.inferencetask.executor.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

public class TaskScheduler {
    private final TaskExecutor executor;
    private final LogRecorder logRecorder;

    public TaskScheduler(TaskExecutor executor, LogRecorder logRecorder) {
        this.executor = executor;
        this.logRecorder = logRecorder;
    }

    /**
     * start container for preparing task
     */

    @Scheduled(fixedDelayString = "${sw.agent.task.schedule.fixedDelay.in.milliseconds:5000}")
    public void dealPreparingTasks() {
        this.executor.dealPreparingTasks();
    }

    /**
     * monitor the status of running task
     */
    @Scheduled(fixedDelayString = "${sw.agent.task.schedule.fixedDelay.in.milliseconds:5000}")
    public void monitorRunningTasks() {
        this.executor.monitorRunningTasks();
    }

    /**
     * do upload
     */
    @Scheduled(fixedDelayString = "${sw.agent.task.schedule.fixedDelay.in.milliseconds:5000}")
    public void uploadResultingTasks() {
        this.executor.uploadTaskResults();
    }

    @Scheduled(fixedDelayString = "${sw.agent.task.schedule.fixedDelay.in.milliseconds:5000}")
    public void reportTasks() {
        this.executor.reportTasks();
    }

    @Scheduled(fixedDelayString = "${sw.agent.task.container.log.schedule.fixedDelay.in.milliseconds:5000}")
    public void logScheduler() {
        this.logRecorder.waitQueueScheduler();
    }
}
