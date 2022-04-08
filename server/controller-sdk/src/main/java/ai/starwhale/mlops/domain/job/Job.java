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
import java.util.Objects;
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
        CREATED(10, false),

        /**
         * split but no task is assigned to an Agent
         */
        RUNNING(20, false),

        /**
         * CANCEL triggered by user( at least one task is TO_CANCEL)
         */
        TO_CANCEL(50, false),

        /**
         * canceling is done
         */
        CANCELED(60, true),

        /**
         * canceling is done
         */
        COLLECT_RESULT(70, false),

        /**
         * all the tasks are finished
         */
        FINISHED(100, true),

        /**
         * some task exit with unexpected error
         */
        EXIT_ERROR(-1, true),

        UNKNOWN(-999, false);
        final int value;
        /**
         * no subsequent statues reported to controller
         */
        final boolean finalStatus;
        JobStatus(int v,boolean finalStatus){
            this.value = v;
            this.finalStatus = finalStatus;
        }
        public int getValue(){
            return this.value;
        }

        public boolean isFinalStatus() {
            return finalStatus;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Job job = (Job) o;
        return uuid.equals(job.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
