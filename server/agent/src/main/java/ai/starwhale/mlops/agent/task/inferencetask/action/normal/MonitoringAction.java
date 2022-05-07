/**
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

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.LogRecorder;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence.ExecuteStatus;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class MonitoringAction extends AbsBasePPLTaskAction {

    @Autowired
    private LogRecorder logRecorder;

    @Override
    public InferenceTask processing(InferenceTask runningTask, Context context)
            throws Exception {
        // dominated by disk(see if other processes have modified)
        InferenceTask newTask = BeanUtil.toBean(runningTask, InferenceTask.class);

        Optional<ExecuteStatus> executeStatus = taskPersistence.status(runningTask.getId());
        if (executeStatus.isPresent()) {
            switch (executeStatus.get()) {
                case start:
                case running:
                case unknown:
                    // nothing to do,just wait
                    break;
                case success:
                    newTask.setStatus(InferenceTaskStatus.UPLOADING);
                    break;
                case failed:
                    newTask.setStatus(InferenceTaskStatus.FAIL);
                    break;
            }
        }
        return newTask;
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {

        if (newTask.getStatus() == InferenceTaskStatus.UPLOADING) {
            taskPool.uploadingTasks.add(newTask);
            // if run success, release device to available device pool todo:is there anything else to do?
            sourcePool.release(newTask.getDevices());
            // only update memory list,there is no need to update the disk file(already update by taskContainer)
            taskPool.runningTasks.remove(oldTask);
            // todo release swmp space and copy it to origin swmp cache dir

        } else if (newTask.getStatus() == InferenceTaskStatus.FAIL) {
            taskPool.failedTasks.add(newTask);
            // if run success, release device to available device pool todo:is there anything else to do?
            sourcePool.release(newTask.getDevices());
            // only update memory list,there is no need to update the disk file(already update by taskContainer)
            taskPool.runningTasks.remove(oldTask);
        } else {
            // try to detect container status
            ContainerClient.ContainerStatus status = containerClient.status(newTask.getContainerId());
            switch (status) {
                case NORMAL:
                    // nothing to do
                    break;
                case DEAD:
                    // todo retry but with serial times
                    if (oldTask.getRetryRestartNum() >= agentProperties.getTask().getRetryRestartMaxNum()) {
                        log.error("task:{} maximum number of restart retries:{} has been reached, task failed",
                                oldTask.getId(), agentProperties.getTask().getRetryRestartMaxNum());
                        sourcePool.release(newTask.getDevices());
                        newTask.setStatus(InferenceTaskStatus.FAIL);
                        taskPool.runningTasks.remove(oldTask);
                        taskPool.failedTasks.add(newTask);
                    } else {
                        log.warn("container:{} is dead, now will restart it", oldTask.getContainerId());
                        oldTask.retryRestart();
                        containerClient.startContainer(oldTask.getContainerId());
                        logRecorder.restart(oldTask.getId(), oldTask.getContainerId());
                    }
                    break;
                case NO_SUCH_CONTAINER:
                    // already be removed or any else error
                    log.error("container:{} may be removed, now will return error", newTask.getContainerId());
                    newTask.setStatus(InferenceTaskStatus.FAIL);
                    taskPool.failedTasks.add(newTask);
                    // if run success, release device to available device pool
                    sourcePool.release(newTask.getDevices());
                    // only update memory list,there is no need to update the disk file(already update by taskContainer)
                    taskPool.runningTasks.remove(oldTask);
                    break;
            }
        }
    }
}
