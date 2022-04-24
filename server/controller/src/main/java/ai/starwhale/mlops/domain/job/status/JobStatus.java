/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job.status;

import ai.starwhale.mlops.domain.job.Job;

/**
 * possible job statuses
 */
public enum JobStatus {
    /**
     * created by user
     */
    CREATED(),

    /**
     * paused by user
     */
    PAUSED(),

    /**
     * split but no task is assigned to an Agent
     */
    RUNNING(),

    /**
     * CANCEL triggered by user( at least one task is TO_CANCEL)
     */
    TO_CANCEL(),

    /**
     * CANCEL triggered by user( at least one task is TO_CANCEL)
     */
    CANCELING(),

    /**
     * canceling is done
     */
    CANCELED(),

    /**
     * all ppl tasks are finished, cmp task should be triggered
     */
    TO_COLLECT_RESULT(),

    /**
     *  cmp task created but not finished
     */
    COLLECTING_RESULT(),

    /**
     * all the tasks are finished
     */
    SUCCESS(),

    /**
     * some task exit with unexpected error
     */
    FAIL(),

    UNKNOWN();

}
