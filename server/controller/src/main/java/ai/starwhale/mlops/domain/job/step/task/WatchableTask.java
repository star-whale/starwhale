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

package ai.starwhale.mlops.domain.job.step.task;

import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskStatusChangeWatcher;
import java.util.List;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * make task status change watchable
 */
@Slf4j
public class WatchableTask extends Task implements TaskWrapper {
    private interface Excludes {
        void updateStatus(TaskStatus status);
    }

    /**
     * original task
     */
    @Delegate(excludes = Excludes.class)
    Task originalTask;

    List<TaskStatusChangeWatcher> watchers;

    public WatchableTask(Task originalTask,
            List<TaskStatusChangeWatcher> watchers) {
        if (originalTask instanceof TaskWrapper) {
            this.originalTask = ((TaskWrapper) originalTask).unwrap();
        } else {
            this.originalTask = originalTask;
        }

        this.watchers = watchers;
    }

    @Override
    public void updateStatus(TaskStatus status) {
        TaskStatus oldStatus = originalTask.getStatus();
        if (oldStatus == status) {
            return;
        }
        if (!TaskStatusMachine.couldTransfer(oldStatus, status)) {
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
