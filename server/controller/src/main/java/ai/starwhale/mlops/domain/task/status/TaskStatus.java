/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.status;

/**
 * possible statuses of a task
 */
public enum TaskStatus {

    /**
     * after created before assigned to an Agent. Ready to be scheduled
     */
    CREATED,

    /**
     * after created before assigned to an Agent. Ready to be scheduled
     */
    ASSIGNING,

    /**
     * pausing triggered by user
     */
    PAUSED,

    /**
     * after assignment is acknowledged before running
     */
    PREPARING,

    /**
     * running
     */
    RUNNING,

    /**
     * garbage is cleared task is finished
     */
    SUCCESS,

    /**
     * cancel triggered by the user
     */
    TO_CANCEL,

    /**
     * agent cancelling task
     */
    CANCELLING,

    /**
     * task canceled success by agent
     */
    CANCELED,

    /**
     * task exit with unexpected error
     */
    FAIL,

    /**
     * UNKNOWN from an Integer
     */
    UNKNOWN();

}
