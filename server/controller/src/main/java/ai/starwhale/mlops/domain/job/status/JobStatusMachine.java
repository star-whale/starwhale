/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job.status;


import static ai.starwhale.mlops.domain.job.status.JobStatus.CANCELED;
import static ai.starwhale.mlops.domain.job.status.JobStatus.CANCELING;
import static ai.starwhale.mlops.domain.job.status.JobStatus.CREATED;
import static ai.starwhale.mlops.domain.job.status.JobStatus.FAIL;
import static ai.starwhale.mlops.domain.job.status.JobStatus.PAUSED;
import static ai.starwhale.mlops.domain.job.status.JobStatus.RUNNING;
import static ai.starwhale.mlops.domain.job.status.JobStatus.SUCCESS;
import static ai.starwhale.mlops.domain.job.status.JobStatus.TO_CANCEL;
import static ai.starwhale.mlops.domain.job.status.JobStatus.UNKNOWN;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JobStatusMachine {

    final static Map<JobStatus, Set<JobStatus>> transferMap = Map.ofEntries(
        new SimpleEntry<>(CREATED, Set.of(PAUSED,RUNNING,SUCCESS,FAIL))
        , new SimpleEntry<>(PAUSED, Set.of(RUNNING))
        , new SimpleEntry<>(RUNNING, Set.of(SUCCESS,FAIL))
        , new SimpleEntry<>(SUCCESS, Set.of())
        , new SimpleEntry<>(FAIL, Set.of())
        , new SimpleEntry<>(TO_CANCEL, Set.of(CANCELING,CANCELED))
        , new SimpleEntry<>(CANCELING, Set.of(CANCELED))
        , new SimpleEntry<>(CANCELED, Set.of())
        , new SimpleEntry<>(UNKNOWN, Set.of()));

    final static Set<JobStatus> finalJobStatuses = Set.of(CANCELED,SUCCESS,FAIL);

    public boolean couldTransfer(JobStatus statusNow,JobStatus statusNew) {
        return transferMap.get(statusNow).contains(statusNew);
    }

    public boolean isFinal(JobStatus status) {
        return finalJobStatuses.contains(status);
    }

}
