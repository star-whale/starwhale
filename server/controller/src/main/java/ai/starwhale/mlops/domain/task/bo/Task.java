/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.common.TimeConcern;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.task.TaskWrapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskType;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
/**
 * Tasks are derived from a Job. Tasks are the executing units of a Job.
 */
public class Task extends TimeConcern {

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
     * the step where the task is derived from
     */
    Step step;

    /**
     * the agent where the task is executed
     */
    Agent agent;

    @Deprecated
    TaskType taskType;

    public void updateStatus(TaskStatus status){
        this.status = status;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void setResultRootPath(ResultPath resultRootPath) {
        this.resultRootPath = resultRootPath;
    }

    public void setTaskRequest(TaskRequest taskRequest) {
        this.taskRequest = taskRequest;
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
        return this.uuid.equals(tsk.uuid);
    }

    public static class StatusUnModifiableTask extends Task implements TaskWrapper {

        Task oTask;
        public StatusUnModifiableTask(Task task){
            this.oTask = task;
        }

        @Override
        public Task unwrap(){
            if(oTask instanceof TaskWrapper){
                TaskWrapper wrappedTask = (TaskWrapper) oTask;
                return wrappedTask.unwrap();
            }
            return oTask;
        }

        @Override
        public int hashCode() {
            return oTask.hashCode();
        }

        @Override
        public boolean equals(Object obj){
            return oTask.equals(obj);
        }

        @Override
        public String toString() {
            return "StatusUnModifiableTask{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                '}';
        }
    }
}
