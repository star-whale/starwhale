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
    CREATED(0x110, JobStatus.SPLIT),

    /**
     * after assigned to an Agent before assignment is acknowledged
     */
    ASSIGNING(0x120, JobStatus.SCHEDULING),

    /**
     * after assignment is acknowledged before running
     */
    PREPARING(0x130, JobStatus.SCHEDULED),

    /**
     * running
     */
    RUNNING(0x140, JobStatus.SCHEDULED),

    /**
     * after task exit normally(container is stopped)
     */
    UPLOADING(0x150, JobStatus.SCHEDULED),


    /**
     * after task exit normally before finished. garbage clearing
     */
    CLOSING(0x160, JobStatus.SCHEDULED),

    /**
     * garbage is cleared
     */
    FINISHED(0xf00, JobStatus.FINISHED),

    /**
     * when report successfully to the controller,it should be archived (Agent only status)
     */
    ARCHIVED(0xf10, JobStatus.FINISHED),

    /**
     * canceling triggered by the user
     */
    CANCEL(0x210, JobStatus.TO_CANCEL),

    /**
     * canceling request sent to Agent before real canceled
     */
    CANCEL_COMMANDING(0x220, JobStatus.TO_CANCEL),

    /**
     * canceling request sent to Agent before real canceled
     */
    CANCELING(0x230, JobStatus.TO_CANCEL),

    /**
     * canceled by the controller
     */
    CANCELED(0x240, JobStatus.CANCELED),

    /**
     * task exit with unexpected error
     */
    EXIT_ERROR(-0x10,JobStatus.EXIT_ERROR),

    /**
     * UNKNOWN from an Integer
     */
    UNKNOWN(-0xf0,JobStatus.UNKNOWN);

    final int order;

    TaskStatus next;

    final JobStatus desiredJobStatus;

    static {
        CREATED.next = ASSIGNING;
        ASSIGNING.next = PREPARING;
        PREPARING.next = RUNNING;
        RUNNING.next = CLOSING;
        CLOSING.next = FINISHED;
        FINISHED.next = FINISHED;
        CANCEL.next = CANCEL_COMMANDING;
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
        for(TaskStatus taskStatus:TaskStatus.values()){
            if(taskStatus.order == v){
                return taskStatus;
            }
        }
        return UNKNOWN;
    }
}
