/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
