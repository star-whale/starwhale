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

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogCollector;
import ai.starwhale.mlops.exception.StarwhaleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(5)
@Slf4j
@Component
public class TaskWatcherForLogging implements TaskStatusChangeWatcher {

    final TaskLogCollector taskLogCollector;

    final TaskStatusMachine taskStatusMachine;

    public TaskWatcherForLogging(
            TaskLogCollector taskLogCollector,
            TaskStatusMachine taskStatusMachine) {
        this.taskLogCollector = taskLogCollector;
        this.taskStatusMachine = taskStatusMachine;
    }

    @Override
    public void onTaskStatusChange(Task task,
            TaskStatus oldStatus) {
        if (!taskStatusMachine.isFinal(task.getStatus()) || task.getStatus() == TaskStatus.CANCELED) {
            log.debug("{} task {} will not collect log", task.getStatus(), task.getId());
            return;
        }
        log.debug("collection log for task {}", task.getId());
        try {
            taskLogCollector.collect(task);
        } catch (StarwhaleException e) {
            log.error("collecting log for task {} error", task.getId(), e);
        }


    }
}
