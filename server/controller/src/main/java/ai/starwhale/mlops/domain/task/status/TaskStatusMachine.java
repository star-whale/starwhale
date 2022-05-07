/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.status;

import static ai.starwhale.mlops.domain.task.status.TaskStatus.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusMachine {

    final static Map<TaskStatus, Set<TaskStatus>> transferMap = Map.ofEntries(
        new SimpleEntry<>(CREATED, Set.of(ASSIGNING,PAUSED,PREPARING,RUNNING,SUCCESS,FAIL,TO_CANCEL,CANCELED))
        , new SimpleEntry<>(PAUSED, Set.of(CREATED))
        , new SimpleEntry<>(ASSIGNING, Set.of(PREPARING,RUNNING,SUCCESS,FAIL,TO_CANCEL))
        , new SimpleEntry<>(PREPARING, Set.of(RUNNING,SUCCESS,FAIL,TO_CANCEL))
        , new SimpleEntry<>(RUNNING, Set.of(SUCCESS,FAIL,TO_CANCEL))
        , new SimpleEntry<>(SUCCESS, Set.of())
        , new SimpleEntry<>(FAIL, Set.of())
        , new SimpleEntry<>(TO_CANCEL, Set.of(CANCELLING,CANCELED,FAIL))
        , new SimpleEntry<>(CANCELLING, Set.of(CANCELLING,CANCELED,FAIL))
        , new SimpleEntry<>(CANCELED, Set.of())
        , new SimpleEntry<>(UNKNOWN, Set.of(TaskStatus.values())));


    public boolean couldTransfer(TaskStatus statusNow, TaskStatus statusNew) {
        return transferMap.get(statusNow).contains(statusNew);
    }

    public boolean isFinal(TaskStatus status) {
        return transferMap.get(status).isEmpty();
    }

}
