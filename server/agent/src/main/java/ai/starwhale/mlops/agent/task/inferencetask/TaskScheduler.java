/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
