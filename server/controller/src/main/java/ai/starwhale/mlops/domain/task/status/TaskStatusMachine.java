/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.status;

import ai.starwhale.mlops.domain.system.StatusMachine;
import ai.starwhale.mlops.domain.task.TaskStatus;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;

public class TaskStatusMachine implements StatusMachine<TaskStatus> {

    final static Set<TaskStatus> finalStatuses = Set.of(TaskStatus.FINISHED, TaskStatus.EXIT_ERROR,
        TaskStatus.CANCELED);

    final static Map<TaskStatus, Set<TaskStatus>> transferMap = Map.ofEntries(
        new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.PAUSED, Set.of())
        , new SimpleEntry<>(TaskStatus.PREPARING, Set.of())
        , new SimpleEntry<>(TaskStatus.RUNNING, Set.of())
        , new SimpleEntry<>(TaskStatus.UPLOADING, Set.of())
        , new SimpleEntry<>(TaskStatus.CLOSING, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of())
        , new SimpleEntry<>(TaskStatus.CREATED, Set.of()));


    @Override
    public boolean couldTransfer(TaskStatus statusNow, TaskStatus statusNew) {
        return false;
    }

    @Override
    public boolean isFinal(TaskStatus status) {
        return finalStatuses.contains(status);
    }
}
