/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job.JobStatus;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Tasks are derived from a Job. Tasks are the executing units of a Job.
 */
@Data
@Builder
public class Task {

    /**
     * unique id for the task
     */
    Long id;

    /**
     * id of the job where the task is derived from
     */
    Long jobId;

    /**
     * random uuid for the task
     */
    String uuid;

    /**
     * status of the task
     */
    TaskStatus status;

    /**
     * storage directory path of results
     */
    String resultPaths;

    private String swdsBlocks;

    /**
     * possible statuses of a task
     */
    public enum TaskStatus{

        /**
         * after created before assigned to an Agent. Ready to be scheduled
         */
        CREATED(110, JobStatus.SPLIT),

        /**
         * after assigned to an Agent before assignment is acknowledged
         */
        ASSIGNING(120, JobStatus.SCHEDULING),

        /**
         * after assignment is acknowledged before running
         */
        PREPARING(130, JobStatus.SCHEDULED),

        /**
         * running
         */
        RUNNING(140, JobStatus.SCHEDULED),

        /**
         * after task exit normally(container is stopped)
         */
        UPLOADING(150, JobStatus.SCHEDULED),


        /**
         * after task exit normally before finished. garbage clearing
         */
        CLOSING(160, JobStatus.SCHEDULED),

        /**
         * garbage is cleared
         */
        FINISHED(1000, JobStatus.FINISHED),

        /**
         * when report successfully to the controller,it should be archived
         */
        ARCHIVED(1010, JobStatus.FINISHED),

        /**
         * canceling triggered by the user
         */
        TO_CANCEL(210, JobStatus.TO_CANCEL),

        /**
         * canceling request sent to Agent before real canceled
         */
        CANCEL_COMMANDING(220, JobStatus.TO_CANCEL),

        /**
         * canceling request sent to Agent before real canceled
         */
        CANCELING(230, JobStatus.TO_CANCEL),

        /**
         * canceled by the controller
         */
        CANCELED(240, JobStatus.CANCELED),

        /**
         * task exit with unexpected error
         */
        EXIT_ERROR(-1,JobStatus.EXIT_ERROR),

        /**
         * UNKNOWN from an Integer
         */
        UNKNOWN(-999,JobStatus.UNKNOWN);

        int order;

        TaskStatus next;

        JobStatus desiredJobStatus;

        static {
            CREATED.next = ASSIGNING;
            ASSIGNING.next = PREPARING;
            PREPARING.next = RUNNING;
            RUNNING.next = CLOSING;
            CLOSING.next = FINISHED;
            FINISHED.next = FINISHED;
            TO_CANCEL.next = CANCEL_COMMANDING;
            CANCEL_COMMANDING.next = CANCELING;
            CANCELING.next = CANCELED;
            CANCELED.next = CANCELED;
            EXIT_ERROR.next = EXIT_ERROR;
        }

        TaskStatus(int order,JobStatus jobStatus){
            this.order = order;
            this.desiredJobStatus = jobStatus;
        }

        public TaskStatus next(){
            return this.next;
        }

        public boolean before(TaskStatus nextStatus){
            return this.order < nextStatus.order;
        }

        public int getOrder(){
            return this.order;
        }

        public JobStatus getDesiredJobStatus(){
            return this.desiredJobStatus;
        }

        public static TaskStatus from(int v){
            for(TaskStatus jobStatus:TaskStatus.values()){
                if(jobStatus.order == v){
                    return jobStatus;
                }
            }
            return UNKNOWN;
        }

    }

    @Override
    public boolean equals(Object obj){

        if(!(obj instanceof Task)){
            return false;
        }
        Task tsk = (Task)obj;
        return this.getId().equals(tsk.getId());
    }
}
