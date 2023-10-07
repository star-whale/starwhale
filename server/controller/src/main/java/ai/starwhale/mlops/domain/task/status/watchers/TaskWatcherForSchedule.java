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
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(6)
public class TaskWatcherForSchedule implements TaskStatusChangeWatcher {

    final SwTaskScheduler swTaskScheduler;

    public TaskWatcherForSchedule(SwTaskScheduler swTaskScheduler) {
        this.swTaskScheduler = swTaskScheduler;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus oldStatus) {
        if (task.getStatus() == TaskStatus.READY || task.getStatus() == TaskStatus.RETRYING) {
            log.debug("task status changed to ready id: {} oldStatus: {}, scheduled", task.getId(), oldStatus);
            swTaskScheduler.schedule(task);
        } else if (task.getStatus() == TaskStatus.CANCELLING || task.getStatus() == TaskStatus.PAUSED) {
            swTaskScheduler.stop(task);
            log.debug("task status changed to {} with id: {} newStatus: {}, stop scheduled immediately",
                    task.getStatus(),
                    task.getId(), task.getStatus());
        } else {
            //do nothing
            log.debug("task {} of status {} do nothing with scheduler ", task.getId(), task.getStatus());
        }
    }


}
