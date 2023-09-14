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

package ai.starwhale.mlops.domain.task.status.watchers;

import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * deal with the data which belong to current task
 */
@Slf4j
@Component
@Order(1)
public class TaskWatcherForDataConsumption implements TaskStatusChangeWatcher {

    final TaskStatusMachine taskStatusMachine;

    final DataLoader dataLoader;

    public TaskWatcherForDataConsumption(
            TaskStatusMachine taskStatusMachine, DataLoader dataLoader) {
        this.taskStatusMachine = taskStatusMachine;
        this.dataLoader = dataLoader;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus oldStatus) {
        log.debug("Handling the task's data for {} ", task.getId());
        TaskStatus status = task.getStatus();

        if (status == TaskStatus.FAIL || status == TaskStatus.CANCELED) {
            // deal with the data which belong to current task TODO: the same with retry logic
            dataLoader.resetUnProcessed(String.valueOf(task.getId()));
        }
    }

}
