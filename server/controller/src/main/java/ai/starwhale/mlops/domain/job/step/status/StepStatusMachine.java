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

package ai.starwhale.mlops.domain.job.step.status;

import static ai.starwhale.mlops.domain.job.step.status.StepStatus.CANCELED;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.CANCELLING;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.CREATED;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.FAIL;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.PAUSED;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.READY;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.RUNNING;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.SUCCESS;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.TO_CANCEL;
import static ai.starwhale.mlops.domain.job.step.status.StepStatus.UNKNOWN;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StepStatusMachine {

    static final Map<StepStatus, Set<StepStatus>> transferMap = Map.of(
            CREATED, Set.of(READY, PAUSED, RUNNING, SUCCESS, TO_CANCEL, CANCELED, FAIL),
            READY, Set.of(PAUSED, RUNNING, SUCCESS, TO_CANCEL, FAIL),
            PAUSED, Set.of(READY, RUNNING, CANCELLING, CANCELED, FAIL),
            RUNNING, Set.of(CANCELLING, CANCELED, PAUSED, SUCCESS, FAIL),
            SUCCESS, Set.of(), FAIL, Set.of(),
            TO_CANCEL, Set.of(CANCELLING, CANCELED, FAIL),
            CANCELLING, Set.of(CANCELED, FAIL), CANCELED, Set.of(),
            UNKNOWN, Set.of(StepStatus.values()));

    public boolean couldTransfer(StepStatus statusNow, StepStatus statusNew) {
        return transferMap.get(statusNow).contains(statusNew);
    }

    public boolean isFinal(StepStatus status) {
        return transferMap.get(status).isEmpty();
    }

}
