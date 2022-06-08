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

import static ai.starwhale.mlops.domain.task.status.TaskStatus.ASSIGNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELLING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.FAIL;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.READY;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.SUCCESS;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import java.util.List;
import java.util.Set;
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
        taskMapper.updateTaskStatus(List.of(task.getId()), task.getStatus());
    }

}
