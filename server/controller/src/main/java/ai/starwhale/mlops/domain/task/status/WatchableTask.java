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

package ai.starwhale.mlops.domain.task.status;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.TaskWrapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * make task status change watchalbe
 */
@Slf4j
public class WatchableTask extends Task implements TaskWrapper {

    /**
     * original task
     */
    @JsonIgnore
    Task oTask;

    @JsonIgnore
    List<TaskStatusChangeWatcher> watchers;

    @JsonIgnore
    TaskStatusMachine taskStatusMachine;

    public WatchableTask(){}

    public WatchableTask(Task oTask,List<TaskStatusChangeWatcher> watchers){
        if(oTask instanceof WatchableTask){
            throw new UnsupportedOperationException();//prevent watchers watched
        }
        this.oTask = oTask;
        this.watchers = watchers;
    }

    @Override
    public Long getId() {
        return oTask.getId();
    }

    @Override
    public String getUuid() {
        return oTask.getUuid();
    }

    @Override
    public TaskStatus getStatus() {
        return oTask.getStatus();
    }

    @Override
    public ResultPath getResultRootPath() {
        return oTask.getResultRootPath();
    }

    @Override
    public TaskRequest getTaskRequest() {
        return oTask.getTaskRequest();
    }

    @Override
    public Job getJob() {
        return oTask.getJob();
    }

    @Override
    public Agent getAgent() {
        return oTask.getAgent();
    }

    @Override
    public TaskType getTaskType() {
        return oTask.getTaskType();
    }

    @Override
    public void setStatus(TaskStatus status){
        watchers.forEach(watcher->watcher.onTaskStatusChange(oTask,status));
    }

    @Override
    public void setId(Long id) {
        oTask.setId(id);
    }

    @Override
    public void setUuid(String uuid) {
        oTask.setUuid(uuid);
    }

    @Override
    public void setResultRootPath(ResultPath resultDir) {
        oTask.setResultRootPath(resultDir);
    }

    @Override
    public void setTaskRequest(TaskRequest taskRequest) {
        oTask.setTaskRequest(taskRequest);
    }

    @Override
    public void setJob(Job job) {
        oTask.setJob(job);
    }

    @Override
    public void setAgent(Agent agent) {
        oTask.setAgent(agent);
    }

    @Override
    public void setTaskType(TaskType taskType) {
        oTask.setTaskType(taskType);
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
    public Task unwrap(){
        if(oTask instanceof TaskWrapper){
            TaskWrapper wrappedTask = (TaskWrapper) oTask;
            return wrappedTask.unwrap();
        }
        return oTask;
    }
}
