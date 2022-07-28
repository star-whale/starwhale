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
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.schedule.CommandingTasksAssurance;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
//@Component
@Order(3)
public class TaskWatcherForCommandingAssurance implements TaskStatusChangeWatcher {

    final CommandingTasksAssurance commandingTasksAssurance;

    final TaskBoConverter taskBoConverter;

    public TaskWatcherForCommandingAssurance(
        CommandingTasksAssurance commandingTasksAssurance,
        TaskBoConverter taskBoConverter) {
        this.commandingTasksAssurance = commandingTasksAssurance;
        this.taskBoConverter = taskBoConverter;
    }

    @Override
    public void onTaskStatusChange(Task task, TaskStatus oldStatus) {
        if(task.getStatus() != TaskStatus.ASSIGNING && task.getStatus() != TaskStatus.CANCELLING){
            log.debug("not commanding task skipped id: {} status:{}",task.getId(),task.getStatus());
            return;
        }
        if(null == task.getAgent()){
            log.error("CODING ERROR: AGENT SHALL BE SET BEFORE TASK STATUS CHANGED TO COMMANDING !!!");
            System.exit(0);
        }
        log.debug("assuring task id: {} status:{} on agent",task.getId(),task.getStatus(),task.getAgent().getSerialNumber());
        commandingTasksAssurance.onTaskCommanding(taskBoConverter.toTaskCommand(List.of(task)),task.getAgent());

    }
}
