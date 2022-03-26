/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

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
     * status of the task
     */
    TaskStatus status;

    /**
     * storage path of results
     */
    List<String> resultPaths;

    /**
     * possible statuses of a task
     */
    public enum TaskStatus{

        /**
         * after created before assigned to an Agent. Ready to be scheduled
         */
        CREATED(1),

        /**
         * after assigned to an Agent before assignment is acknowledged
         */
        ASSIGNING(2),

        /**
         * after assignment is acknowledged before running
         */
        PREPARING(3),

        /**
         * running
         */
        RUNNING(4),

        /**
         * after task exit normally(container is stopped)
         */
        UPLOADING(5),


        /**
         * after task exit normally before finished. garbage clearing
         */
        CLOSING(6),

        /**
         * garbage is cleared
         */
        FINISHED(100),

        /**
         * when report successfully to the controller,it should be archived
         */
        ARCHIVED(101),

        /**
         * canceling triggered by the user
         */
        TO_CANCEL(10),

        /**
         * canceling request sent to Agent before real canceled
         */
        CANCEL_COMMANDING(11),

        /**
         * canceling request sent to Agent before real canceled
         */
        CANCELING(12),

        /**
         * canceled by the controller
         */
        CANCELED(13),

        /**
         * task exit with unexpected error
         */
        EXIT_ERROR(-1);

        int order;

        TaskStatus next;

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

        TaskStatus(int order){
            this.order = order;
        }

        public TaskStatus next(){
            return this.next;
        }

        public boolean before(TaskStatus nextStatus){
            return this.order < nextStatus.order;
        }

    }

    public boolean equals(Object obj){

        if(!(obj instanceof Task)){
            return false;
        }
        Task tsk = (Task)obj;
        return this.getId().equals(tsk.getId());
    }
}
