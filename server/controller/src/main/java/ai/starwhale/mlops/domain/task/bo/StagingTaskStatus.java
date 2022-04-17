/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.task.TaskStatus;
import java.util.Objects;

public class StagingTaskStatus implements Comparable<StagingTaskStatus>{
    final TaskStatus status;
    final TaskStatusStage stage;

    public StagingTaskStatus(){
        this.status = TaskStatus.CREATED;
        this.stage = TaskStatusStage.INIT;
    }

    public StagingTaskStatus(TaskStatus status){
        this.status = status;
        this.stage = TaskStatusStage.INIT;
    }

    public StagingTaskStatus(TaskStatus status,TaskStatusStage stage){
        this.status = status;
        this.stage = stage;
    }

    public TaskStatusStage getStage() {
        return stage;
    }

    public StagingTaskStatus stage(TaskStatusStage stage) {
        return new StagingTaskStatus(this.status,stage);
    }

    public StagingTaskStatus clearStage() {
        return new StagingTaskStatus(this.status,TaskStatusStage.INIT);
    }

    public int getValue(){
        return status.getOrder() + stage.getValue();
    }

    public TaskStatus getStatus(){
        return status;
    }

    public static StagingTaskStatus from(int v){
        final TaskStatus status = TaskStatus.from(v & 0xff0);
        TaskStatusStage stage = TaskStatusStage.from(v & 0xf);
        return new StagingTaskStatus(status,stage);
    }

    public boolean before(StagingTaskStatus nextStatus){
        return compareTo(nextStatus)<0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StagingTaskStatus that = (StagingTaskStatus) o;
        return status == that.status &&
            stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public int compareTo(StagingTaskStatus o) {
        return this.getValue() - o.getValue();
    }

    public JobStatus getDesiredJobStatus() {
        if(this.status == TaskStatus.FINISHED && this.stage == TaskStatusStage.DONE){
            return JobStatus.COLLECT_RESULT;
        }
        if(this.stage == TaskStatusStage.FAILED){
            return JobStatus.EXIT_ERROR;
        }
        return this.status.getDesiredJobStatus();
    }
}
