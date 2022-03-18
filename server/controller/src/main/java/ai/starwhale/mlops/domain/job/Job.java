/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
@Builder
public class Job {

    Long id;

    /**
     * the SWDSs to run on
     */
    List<SWDataSet> swDataSets;

    /**
     * SWMP to be run
     */
    SWModelPackage swmp;

    /**
     * runtime info of the job
     */
    JobRuntime jobRuntime;

    /**
     * the status of a job
     */
    JobStatus status;

    /**
     * possible statuses of a job
     */
    public enum JobStatus{

        /**
         * created by user
         */
        CREATED,

        /**
         * split but no task is assigned to an Agent
         */
        SPLIT,

        /**
         * more than one task is assigned to an Agent
         */
        SCHEDULING,

        /**
         * all tasks are assigned to Agents, but not all of them is finished
         */
        SCHEDULED,

        /**
         * all the tasks are finished
         */
        FINISHED
    }

}
