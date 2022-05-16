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

package ai.starwhale.mlops.domain.job.status;


import static ai.starwhale.mlops.domain.job.status.JobStatus.CANCELED;
import static ai.starwhale.mlops.domain.job.status.JobStatus.CANCELING;
import static ai.starwhale.mlops.domain.job.status.JobStatus.COLLECTING_RESULT;
import static ai.starwhale.mlops.domain.job.status.JobStatus.CREATED;
import static ai.starwhale.mlops.domain.job.status.JobStatus.FAIL;
import static ai.starwhale.mlops.domain.job.status.JobStatus.PAUSED;
import static ai.starwhale.mlops.domain.job.status.JobStatus.RUNNING;
import static ai.starwhale.mlops.domain.job.status.JobStatus.SUCCESS;
import static ai.starwhale.mlops.domain.job.status.JobStatus.TO_CANCEL;
import static ai.starwhale.mlops.domain.job.status.JobStatus.TO_COLLECT_RESULT;
import static ai.starwhale.mlops.domain.job.status.JobStatus.UNKNOWN;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JobStatusMachine {

    final static Map<JobStatus, Set<JobStatus>> transferMap = Map.ofEntries(
        new SimpleEntry<>(CREATED, Set.of(PAUSED,RUNNING,SUCCESS,TO_CANCEL,FAIL))
        , new SimpleEntry<>(PAUSED, Set.of(RUNNING,CANCELED,FAIL))
        , new SimpleEntry<>(RUNNING, Set.of(PAUSED,TO_COLLECT_RESULT,SUCCESS,FAIL))
        , new SimpleEntry<>(TO_COLLECT_RESULT, Set.of(COLLECTING_RESULT,SUCCESS,TO_CANCEL,FAIL))
        , new SimpleEntry<>(COLLECTING_RESULT, Set.of(SUCCESS,TO_CANCEL,FAIL))
        , new SimpleEntry<>(SUCCESS, Set.of())
        , new SimpleEntry<>(FAIL, Set.of())
        , new SimpleEntry<>(TO_CANCEL, Set.of(CANCELING,CANCELED,FAIL))
        , new SimpleEntry<>(CANCELING, Set.of(CANCELED,FAIL))
        , new SimpleEntry<>(CANCELED, Set.of())
        , new SimpleEntry<>(UNKNOWN, Set.of(JobStatus.values())));

    public boolean couldTransfer(JobStatus statusNow,JobStatus statusNew) {
        return transferMap.get(statusNow).contains(statusNew);
    }

    public boolean isFinal(JobStatus status) {
        return transferMap.get(status).isEmpty();
    }

}

