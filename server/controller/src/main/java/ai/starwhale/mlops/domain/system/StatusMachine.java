/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.domain.job.Job.JobStatus;

/**
 * manage status
 */
public interface StatusMachine<S> {
    boolean couldTransfer(S statusNow,S statusNew);
    boolean isFinal(S status);
}
