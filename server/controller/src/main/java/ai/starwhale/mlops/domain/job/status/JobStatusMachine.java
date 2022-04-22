/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job.status;

import ai.starwhale.mlops.common.Reduceable;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.system.StatusMachine;
import java.util.Collection;

public class JobStatusMachine implements StatusMachine<JobStatus>, Reduceable<JobStatus> {

    public boolean couldTransfer(JobStatus statusNow,JobStatus statusNew) {
        return false;
    }
    public boolean isFinal(JobStatus status) {
        return false;
    }

    @Override
    public JobStatus reduce(Collection<JobStatus> collection) {
        return null;
    }
}
