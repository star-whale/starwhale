/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task;

import ai.starwhale.mlops.agent.task.executor.TaskExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sw.task.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class Scheduler {
    private final TaskExecutor executor;

    public Scheduler(TaskExecutor executor) {
        this.executor = executor;
    }

    /**
     * allocate device(GPU or CPU) for task
     */
    /*@Scheduled(fixedDelay = 10000)
    public void allocateDeviceForPreparingTasks() {
        this.executor.allocateDeviceForPreparingTasks();
    }*/

    /**
     * start container for preparing task
     */

    @Scheduled(fixedDelay = 5000)
    public void dealPreparingTasks() {
        this.executor.dealPreparingTasks();
    }

    /**
     * monitor the status of running task
     */
    @Scheduled(fixedDelay = 5000)
    public void monitorRunningTasks() {
        this.executor.monitorRunningTasks();
    }

    /**
     * do upload
     */
    @Scheduled(fixedDelay = 5000)
    public void uploadResultingTasks() {
        this.executor.uploadTaskResults();
    }

    @Scheduled(fixedDelay = 5000)
    public void reportTasks() {
        this.executor.reportTasks();
    }
}
