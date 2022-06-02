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
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.TaskWrapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * make task status change watchalbe
 */
@Slf4j
public class WatchableTask extends Task implements TaskWrapper {

    /**
     * original task
     */
    Task oTask;

    List<TaskStatusChangeWatcher> watchers;

    TaskStatusMachine taskStatusMachine;

    public WatchableTask(Task oTask,List<TaskStatusChangeWatcher> watchers,TaskStatusMachine taskStatusMachine){
        if(oTask instanceof WatchableTask){
            throw new UnsupportedOperationException();//prevent watchers watched
        }
        this.oTask = oTask;
        this.watchers = watchers;
        this.taskStatusMachine = taskStatusMachine;
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
    public Step getStep() {
        return oTask.getStep();
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
    public void updateStatus(TaskStatus status){
        TaskStatus oldStatus = oTask.getStatus();
        if(oldStatus == status){
            return;
        }
        if(!taskStatusMachine.couldTransfer(oldStatus, status)){
            log.warn("task status changed unexpectedly from {} to {}  of id {} forbidden",oldStatus,status,oTask.getId());
            return;
        }
        oTask.updateStatus(status);
        log.debug("task status changed from {} to {}  of id {}",oldStatus,status,oTask.getId());
        watchers.stream().filter(w -> {
                if (TaskStatusChangeWatcher.APPLIED_WATCHERS.get() == null) {
                    log.debug("not watchers selected default to all");
                    return true;
                }
                Order annotation = w.getClass().getAnnotation(Order.class);
                if (null == annotation) {
                    return false;
                }
                return TaskStatusChangeWatcher.APPLIED_WATCHERS.get().contains(annotation.value());
            }
        ).forEach(watcher -> watcher.onTaskStatusChange(this, oldStatus));
    }

    @Override
    public void setAgent(Agent agent) {
        oTask.setAgent(agent);
    }

    public void setResultRootPath(ResultPath resultRootPath) {
        oTask.setResultRootPath(resultRootPath);
    }

    public void setTaskRequest(TaskRequest taskRequest) {
        oTask.setTaskRequest(taskRequest);
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
