/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.agent.task.inferencetask;

/**
 * status of a task
 */
public enum InferenceTaskStatus {

    /**
     * after assignment is acknowledged before running
     */
    PREPARING,

    /**
     * running
     */
    RUNNING,

    /**
     * after task exit normally(container is stopped)
     */
    UPLOADING,

    /**
     *
     */
    SUCCESS,

    /**
     * task exit with unexpected error
     */
    FAIL,

    /**
     * canceling triggered by the user
     */
    CANCELING,

    /**
     * task canceled success by agent
     */
    CANCELED,

    /**
     * when report successfully to the controller,it should be archived (Agent only status)
     */
    ARCHIVED

}
