/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.domain.task;

import lombok.Data;

/**
 * Tasks are derived from a Job. Tasks are the executing units of a Job.
 */
@Data
public class Task {

    /**
     * unique id for the task
     */
    String id;

    /**
     * id of the job where the task is derived from
     */
    String jobId;

    /**
     * status of the task
     */
    TaskStatus status;

    /**
     * possible statuses of a task
     */
    public enum TaskStatus{

        /**
         * after created before assigned to an Agent
         */
        CREATED,

        /**
         * after assigned to an Agent before running
         */
        PREPARING,

        /**
         * running
         */
        RUNNING,

        /**
         * after task exit normally before closed. garbage clearing
         */
        CLOSING,

        /**
         * garbage is cleared
         */
        CLOSED,

        /**
         * canceled by the controller
         */
        CANCELED,

        /**
         * task exit with unexpected error
         */
        EXIT_ERROR

    }
}
