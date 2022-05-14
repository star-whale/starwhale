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
import static ai.starwhale.mlops.domain.task.status.TaskStatus.SUCCESS;

import ai.starwhale.mlops.domain.task.cache.LivingTaskCache;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * update status in cache, persist easy lost tasks
 */
@Component
@Order(1)
public class TaskWatcherForCache implements TaskStatusChangeWatcher {

    final TaskStatusMachine taskStatusMachine;

    final TaskMapper taskMapper;

    final LivingTaskCache taskCache;

    public TaskWatcherForCache(
        TaskStatusMachine taskStatusMachine,
        TaskMapper taskMapper, LivingTaskCache taskCache) {
        this.taskStatusMachine = taskStatusMachine;
        this.taskMapper = taskMapper;
        this.taskCache = taskCache;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus newStatus) {
        if(task.getStatus() == newStatus){
            return;
        }
        if(easyLost(newStatus)){
            taskMapper.updateTaskStatus(List.of(task.getId()),newStatus);
        }
        taskCache.update(task.getId(),newStatus);
    }

    final static Set<TaskStatus> easyLostStatuses = Set.of(SUCCESS, FAIL,
        CANCELED,ASSIGNING,CANCELLING);
    private boolean easyLost(TaskStatus status){
        return easyLostStatuses.contains(status);
    }
}
