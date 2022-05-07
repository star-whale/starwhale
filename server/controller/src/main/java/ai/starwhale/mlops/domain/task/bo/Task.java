/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskType;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tasks are derived from a Job. Tasks are the executing units of a Job.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Task {

    /**
     * unique id for the task
     */
    Long id;

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
    ResultPath resultRootPath;

    TaskRequest taskRequest;

    /**
     * the job where the task is derived from
     */
    Job job;

    /**
     * the agent where the task is executed
     */
    Agent agent;

    TaskType taskType;

    public Task statusUnModifiable(){
        return new StatusUnModifiableTask(this);
    }


    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Task)){
            return false;
        }
        Task tsk = (Task)obj;
        return this.getUuid().equals(tsk.getUuid());
    }



    public static class StatusUnModifiableTask extends Task{

        Task oTask;
        public StatusUnModifiableTask(Task task){
            this.oTask = task;
        }

        @Override
        public Long getId() {
            return oTask.id;
        }

        @Override
        public String getUuid() {
            return oTask.uuid;
        }

        @Override
        public TaskStatus getStatus() {
            return oTask.status;
        }

        @Override
        public ResultPath getResultRootPath() {
            return oTask.resultRootPath;
        }

        @Override
        public TaskRequest getTaskRequest() {
            return oTask.taskRequest;
        }

        @Override
        public Job getJob() {
            return oTask.job;
        }

        @Override
        public Agent getAgent() {
            return oTask.agent;
        }

        @Override
        public TaskType getTaskType() {
            return oTask.taskType;
        }

        @Override
        public void setStatus(TaskStatus status){
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setUuid(String uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResultRootPath(ResultPath resultDir) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTaskRequest(TaskRequest taskRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setJob(Job job) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAgent(Agent agent) {
            oTask.agent = agent;
        }

        @Override
        public void setTaskType(TaskType taskType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return oTask.hashCode();
        }

        @Override
        public boolean equals(Object obj){
            return oTask.equals(obj);
        }
    }
}
