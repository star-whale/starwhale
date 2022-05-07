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

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * convert taskReport to
 */
@AllArgsConstructor
public class ReportedTask {

    final static Map<TaskStatusInterface, TaskStatus> transferMapIn = Map.ofEntries(
        new SimpleEntry<>(TaskStatusInterface.CANCELED, TaskStatus.CANCELED)
        , new SimpleEntry<>(TaskStatusInterface.CANCELING, TaskStatus.CANCELLING)
        , new SimpleEntry<>(TaskStatusInterface.PREPARING, TaskStatus.PREPARING)
        , new SimpleEntry<>(TaskStatusInterface.RUNNING, TaskStatus.RUNNING)
        , new SimpleEntry<>(TaskStatusInterface.FAIL, TaskStatus.FAIL)
        , new SimpleEntry<>(TaskStatusInterface.SUCCESS, TaskStatus.SUCCESS));

    final Long id;
    final TaskStatus status;
    final TaskType taskType;
    public static ReportedTask from(TaskReport taskReport){
        return new ReportedTask(taskReport.getId(),transferMapIn.get(taskReport.getStatus()),taskReport.getTaskType());
    }

    public Long getId() {
        return id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskType getTaskType() {
        return taskType;
    }
}
