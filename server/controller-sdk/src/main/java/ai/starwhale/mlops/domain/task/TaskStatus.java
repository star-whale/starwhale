/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job.JobStatus;

/**
 * possible statuses of a task
 */
public enum TaskStatus {
    /**
     * after created before assigned to an Agent. Ready to be scheduled
     * init: new created
     * doing: assigning to agent
     * done: assigned to agent
     */
    CREATED(0x110, JobStatus.RUNNING, false),

    /**
     * pausing triggered by user
     */
    PAUSED(0x120, JobStatus.PAUSED, false),

    /**
     * after assignment is acknowledged before running
     */
    PREPARING(0x130, JobStatus.RUNNING, false),

    /**
     * running
     */
    RUNNING(0x140, JobStatus.RUNNING, false),

    /**
     * after task exit normally(container is stopped)
     */
    UPLOADING(0x150, JobStatus.RUNNING, false),


    /**
     * after task exit normally before finished. garbage clearing
     */
    CLOSING(0x160, JobStatus.RUNNING, false),

    /**
     * garbage is cleared task is finished
     */
    FINISHED(0xf00, JobStatus.TO_COLLECT_RESULT, true),

    /**
     * when report successfully to the controller,it should be archived (Agent only status)
     */
    ARCHIVED(0xf10, JobStatus.TO_COLLECT_RESULT, true),

    /**
     * canceling triggered by the user
     */
    CANCEL(0x210, JobStatus.TO_CANCEL, false),

    /**
     * task canceled success by agent
     */
    CANCELED(0x220, JobStatus.CANCELED, true),

    /**
     * task exit with unexpected error
     */
    EXIT_ERROR(0xfff,JobStatus.EXIT_ERROR, true),

    /**
     * UNKNOWN from an Integer
     */
    UNKNOWN(-0xf0,JobStatus.UNKNOWN, false);

    final int order;

    final JobStatus desiredJobStatus;

    /**
     * no subsequent statues reported to controller
     */
    final boolean finalStatus;

    TaskStatus(int order,JobStatus jobStatus,boolean finalStatus){
        this.order = order;
        this.desiredJobStatus = jobStatus;
        this.finalStatus = finalStatus;
    }

    public boolean isFinalStatus() {
        return finalStatus;
    }

    public int getOrder(){
        return this.order;
    }

    public JobStatus getDesiredJobStatus(){
        return this.desiredJobStatus;
    }

    public static TaskStatus from(int v){
        for(TaskStatus taskStatus:TaskStatus.values()){
            if(taskStatus.order == v){
                return taskStatus;
            }
        }
        return UNKNOWN;
    }
}
