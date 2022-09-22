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
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * update status in cache, persist easy lost tasks
 */
@Slf4j
@Component
@Order(1)
public class TaskWatcherForPersist implements TaskStatusChangeWatcher {

    final TaskStatusMachine taskStatusMachine;

    final TaskMapper taskMapper;

    public TaskWatcherForPersist(
            TaskStatusMachine taskStatusMachine,
            TaskMapper taskMapper) {
        this.taskStatusMachine = taskStatusMachine;
        this.taskMapper = taskMapper;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus oldStatus) {
        log.debug("persisting task for {} ", task.getId());
        TaskStatus status = task.getStatus();
        var now = new Date();
        if (taskStatusMachine.isFinal(status)) {
            task.setFinishTime(now.getTime());
            taskMapper.updateTaskFinishedTime(task.getId(), now);
        }
        if (status == TaskStatus.RUNNING) {
            task.setStartTime(now.getTime());
            taskMapper.updateTaskStartedTime(task.getId(), now);
        }
        taskMapper.updateTaskStatus(List.of(task.getId()), status);
        log.debug("task {} status persisted to {} ", task.getId(), status);
    }

}
