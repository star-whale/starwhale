/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    Long id;

    String uuid;

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
        CREATED(10),

        /**
         * split but no task is assigned to an Agent
         */
        SPLIT(20),

        /**
         * more than one task is assigned to Agent
         */
        SCHEDULING(30),

        /**
         * all tasks are assigned to Agents, but not all of them is finished
         */
        SCHEDULED(40),

        /**
         * CANCEL triggered by user( at least one task is TO_CANCEL)
         */
        TO_CANCEL(50),

        /**
         * canceling is done
         */
        CANCELED(60),

        /**
         * all the tasks are finished
         */
        FINISHED(100),

        /**
         * some task exit with unexpected error
         */
        EXIT_ERROR(-1),

        UNKNOWN(-999);
        final int value;
        JobStatus(int v){
            this.value = v;
        }
        public int getValue(){
            return this.value;
        }
        public static JobStatus from(int v){
            for(JobStatus jobStatus:JobStatus.values()){
                if(jobStatus.value == v){
                    return jobStatus;
                }
            }
            return UNKNOWN;
        }
        public boolean before(JobStatus nextStatus){
            return this.value < nextStatus.value;
        }
    }



}
