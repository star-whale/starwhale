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

package ai.starwhale.mlops.domain.task.status;

import static ai.starwhale.mlops.domain.task.status.TaskStatus.ASSIGNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELLING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CREATED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.FAIL;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PAUSED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PREPARING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.READY;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.RETRYING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.RUNNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.SUCCESS;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.UNKNOWN;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskStatusMachine {

    static final Map<TaskStatus, Set<TaskStatus>> transferMap = Map.ofEntries(
            new SimpleEntry<>(CREATED, Set.of(ASSIGNING, READY, PREPARING, RUNNING, RETRYING, SUCCESS, FAIL, CANCELED)),
            new SimpleEntry<>(READY, Set.of(ASSIGNING, PAUSED, PREPARING, RUNNING, RETRYING, SUCCESS, FAIL, CANCELED)),
            new SimpleEntry<>(
                    RETRYING,
                    Set.of(ASSIGNING, PAUSED, PREPARING, RUNNING, RETRYING, SUCCESS, FAIL, CANCELED)
            ),
            new SimpleEntry<>(PAUSED, Set.of(PREPARING, ASSIGNING, RUNNING, RETRYING, READY, CANCELED, SUCCESS)),
            new SimpleEntry<>(ASSIGNING, Set.of(CREATED, PREPARING, RUNNING, RETRYING, SUCCESS, FAIL, CANCELLING)),
            new SimpleEntry<>(PREPARING, Set.of(RUNNING, RETRYING, SUCCESS, FAIL, CANCELLING, CANCELED)),
            new SimpleEntry<>(RUNNING, Set.of(SUCCESS, RETRYING, FAIL, CANCELLING, CANCELED)),
            new SimpleEntry<>(CANCELLING, Set.of(CANCELED, FAIL)),
            new SimpleEntry<>(CANCELED, Set.of()),
            new SimpleEntry<>(SUCCESS, Set.of()),
            new SimpleEntry<>(FAIL, Set.of()),
            new SimpleEntry<>(UNKNOWN, Set.of(TaskStatus.values()))
    );

    static final Map<TaskStatus, Map<RunStatus, TaskStatus>> transferRunMap = Map.ofEntries(
            new SimpleEntry<>(
                    CREATED,
                    Map.of(
                            RunStatus.PENDING,
                            PREPARING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(
                    READY,
                    Map.of(
                            RunStatus.PENDING,
                            PREPARING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(
                    PAUSED,
                    Map.of(
                            RunStatus.PENDING,
                            PREPARING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(
                    RETRYING,
                    Map.of(
                            RunStatus.PENDING,
                            RETRYING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(
                    PREPARING,
                    Map.of(
                            RunStatus.PENDING,
                            PREPARING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(
                    RUNNING,
                    Map.of(
                            RunStatus.PENDING,
                            RUNNING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(
                    CANCELLING,
                    Map.of(
                            RunStatus.PENDING,
                            CANCELLING,
                            RunStatus.RUNNING,
                            CANCELLING,
                            RunStatus.FAILED,
                            CANCELED,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            ),
            new SimpleEntry<>(CANCELED, Map.of()),
            new SimpleEntry<>(SUCCESS, Map.of()),
            new SimpleEntry<>(FAIL, Map.of()),
            new SimpleEntry<>(
                    UNKNOWN,
                    Map.of(
                            RunStatus.PENDING,
                            PREPARING,
                            RunStatus.RUNNING,
                            RUNNING,
                            RunStatus.FAILED,
                            FAIL,
                            RunStatus.FINISHED,
                            SUCCESS
                    )
            )
    );

    public boolean couldTransfer(TaskStatus statusNow, TaskStatus statusNew) {
        return transferMap.get(statusNow).contains(statusNew);
    }

    public TaskStatus transfer(TaskStatus statusNow, TaskStatus statusNew) {
        if (statusNow == CANCELLING) {
            if (isFinal(statusNew)) {
                return CANCELED;
            } else {
                return CANCELLING;
            }
        }
        return statusNew;
    }

    public TaskStatus transfer(TaskStatus statusNow, RunStatus runStatus) {
        Map<RunStatus, TaskStatus> transferMap = transferRunMap.get(statusNow);
        TaskStatus desiredStatus = transferMap.get(runStatus);
        if (null == desiredStatus) {
            log.warn(
                    "TaskStatusMachine.transfer: there shouldn't be a runStatus: {} for statusNow: {}",
                    statusNow,
                    runStatus
            );
            return statusNow;
        }
        return desiredStatus;
    }

    public boolean isFinal(TaskStatus status) {
        return transferMap.get(status).isEmpty();
    }

}
