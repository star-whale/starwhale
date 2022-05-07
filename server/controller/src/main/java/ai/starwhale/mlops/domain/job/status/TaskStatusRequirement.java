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

package ai.starwhale.mlops.domain.job.status;

import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TaskStatusRequirement {

    Set<TaskStatus> taskStatuses;

    /**
     * if null, no constrain on this field
     */
    TaskType taskType;

    RequireType requireType;

    public enum RequireType {
        /**
         * if any of the tasks fit the  taskStatus, the requirement is meet
         */
        ANY,
        /**
         * if not all the tasks fit the taskStatus, the requirement is not meet
         */
        ALL,
        /**
         * if any of the tasks fit the  taskStatus (status in taskStatuses), the requirement is not meet
         */
        HAVE_NO,
        /**
         * if no task fit the  taskStatus (status in taskStatuses), the requirement is not meet
         */
        MUST;
    }

    public boolean fit(Collection<Task> tasks){
        Collection<Task> tobeCheckedTasks = tasks;
        if(taskType != null){
            tobeCheckedTasks = tasks.parallelStream().filter(t -> t.getTaskType() == taskType)
                .collect(Collectors.toList());
        }
        List<TaskStatus> taskStatusList = tobeCheckedTasks.parallelStream()
            .map(Task::getStatus).collect(Collectors.toList());
        switch (requireType){
            case ANY:
            case MUST:
                return taskStatusList.stream().anyMatch(ts -> getTaskStatuses().contains(ts));
            case ALL:
                return taskStatusList.stream().allMatch(ts -> getTaskStatuses().contains(ts));
            case HAVE_NO:
                return taskStatusList.stream().allMatch(ts -> !getTaskStatuses().contains(ts));
            default:
                return false;

        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskStatusRequirement that = (TaskStatusRequirement) o;
        return taskStatuses == that.taskStatuses && taskType == that.taskType
            && requireType == that.requireType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskStatuses, taskType, requireType);
    }
}
