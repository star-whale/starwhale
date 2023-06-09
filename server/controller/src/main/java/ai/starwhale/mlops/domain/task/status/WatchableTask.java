/*
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

package ai.starwhale.mlops.domain.task.status;

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.TaskWrapper;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * make task status change watchable
 */
@Slf4j
public class WatchableTask extends Task implements TaskWrapper {

    /**
     * original task
     */
    Task originalTask;

    List<TaskStatusChangeWatcher> watchers;

    TaskStatusMachine taskStatusMachine;

    public WatchableTask(Task originalTask,
            List<TaskStatusChangeWatcher> watchers,
            TaskStatusMachine taskStatusMachine) {
        if (originalTask instanceof TaskWrapper) {
            this.originalTask = ((TaskWrapper) originalTask).unwrap();
        } else {
            this.originalTask = originalTask;
        }

        this.watchers = watchers;
        this.taskStatusMachine = taskStatusMachine;
    }

    @Override
    public Long getId() {
        return originalTask.getId();
    }

    @Override
    public String getUuid() {
        return originalTask.getUuid();
    }

    @Override
    public TaskStatus getStatus() {
        return originalTask.getStatus();
    }

    @Override
    public ResultPath getResultRootPath() {
        return originalTask.getResultRootPath();
    }

    @Override
    public TaskRequest getTaskRequest() {
        return originalTask.getTaskRequest();
    }

    @Override
    public Step getStep() {
        return originalTask.getStep();
    }

    @Override
    public Long getStartTime() {
        return originalTask.getStartTime();
    }

    @Override
    public Long getFinishTime() {
        return originalTask.getFinishTime();
    }

    @Override
    public void updateStatus(TaskStatus status) {
        TaskStatus oldStatus = originalTask.getStatus();
        if (oldStatus == status) {
            return;
        }
        if (!taskStatusMachine.couldTransfer(oldStatus, status)) {
            log.warn("task status changed unexpectedly from {} to {}  of id {} ",
                    oldStatus, status, originalTask.getId());
            return;
        }
        originalTask.updateStatus(status);
        log.debug("task status changed from {} to {}  of id {}", oldStatus, status, originalTask.getId());
        watchers.stream().filter(w -> {
                    if (TaskStatusChangeWatcher.SKIPPED_WATCHERS.get() == null) {
                        log.debug("not watchers selected default to all");
                        return true;
                    }
                    return !TaskStatusChangeWatcher.SKIPPED_WATCHERS.get().contains(w.getClass());
                }
        ).forEach(watcher -> watcher.onTaskStatusChange(originalTask, oldStatus));
    }

    @Override
    public void setRetryNum(Integer retryNum) {
        originalTask.setRetryNum(retryNum);
    }

    @Override
    public void setStartTime(Long startTime) {
        originalTask.setStartTime(startTime);
    }

    @Override
    public void setFinishTime(Long finishTime) {
        originalTask.setFinishTime(finishTime);
    }

    public void setResultRootPath(ResultPath resultRootPath) {
        originalTask.setResultRootPath(resultRootPath);
    }

    public void setTaskRequest(TaskRequest taskRequest) {
        originalTask.setTaskRequest(taskRequest);
    }

    public void setIp(String ip) {
        originalTask.setIp(ip);
    }

    @Override
    public int hashCode() {
        return originalTask.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return originalTask.equals(obj);
    }

    @Override
    public Task unwrap() {
        if (originalTask instanceof TaskWrapper) {
            TaskWrapper wrappedTask = (TaskWrapper) originalTask;
            return wrappedTask.unwrap();
        }
        return originalTask;
    }
}
