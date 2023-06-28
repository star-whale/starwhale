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

import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELLING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CREATED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.FAIL;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PAUSED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PREPARING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.READY;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.RUNNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.SUCCESS;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.UNKNOWN;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusMachine {

    static final Map<TaskStatus, Set<TaskStatus>> transferMap = Map.ofEntries(
            new SimpleEntry<>(CREATED, Set.of(READY, PREPARING, RUNNING, CANCELLING, CANCELED, SUCCESS, FAIL)),
            new SimpleEntry<>(READY, Set.of(PAUSED, PREPARING, RUNNING, CANCELLING, CANCELED, SUCCESS, FAIL)),
            new SimpleEntry<>(PAUSED, Set.of(READY, PREPARING, RUNNING, CANCELLING, CANCELED, SUCCESS, FAIL)),
            new SimpleEntry<>(PREPARING, Set.of(RUNNING, CANCELLING, CANCELED, SUCCESS, FAIL)),
            new SimpleEntry<>(RUNNING, Set.of(CANCELLING, CANCELED, SUCCESS, FAIL)),
            new SimpleEntry<>(CANCELLING, Set.of(CANCELED, SUCCESS, FAIL)),
            new SimpleEntry<>(CANCELED, Set.of()),
            new SimpleEntry<>(SUCCESS, Set.of()),
            new SimpleEntry<>(FAIL, Set.of()),
            new SimpleEntry<>(UNKNOWN, Set.of(TaskStatus.values())));


    public boolean couldTransfer(TaskStatus statusNow, TaskStatus statusNew) {
        return transferMap.get(statusNow).contains(statusNew);
    }

    public boolean isFinal(TaskStatus status) {
        return transferMap.get(status).isEmpty();
    }

}
