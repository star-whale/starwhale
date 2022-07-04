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
public class MonitoringAction extends AbsBaseTaskAction {

    @Autowired
    private LogRecorder logRecorder;

    @Override
    public InferenceTask processing(InferenceTask runningTask, Context context)
            throws Exception {

        Optional<ExecuteStatus> executeStatus = taskPersistence.status(runningTask.getId());
        if (executeStatus.isPresent()) {
            switch (executeStatus.get()) {
                case start:
                case running:
                case unknown:
                    // nothing to do,just wait
                    break;
                case success:
                    runningTask.setStatus(InferenceTaskStatus.UPLOADING);
                    break;
                case failed:
                    runningTask.setStatus(InferenceTaskStatus.FAIL);
                    break;
            }
        }
        return runningTask;
    }

    @Override
    public void success(InferenceTask originTask, InferenceTask newTask, Context context) {

        if (newTask.getStatus() == InferenceTaskStatus.UPLOADING) {
            taskPool.uploadingTasks.add(newTask);
            // if run success, release device to available device pool todo:is there anything else to do?
            sourcePool.release(newTask.getDevices());
            // only update memory list,there is no need to update the disk file(already update by taskContainer)
            taskPool.runningTasks.remove(originTask);
            // todo release swmp space and copy it to origin swmp cache dir

        } else if (newTask.getStatus() == InferenceTaskStatus.FAIL) {
            taskPool.failedTasks.add(newTask);
            // if run success, release device to available device pool todo:is there anything else to do?
            sourcePool.release(newTask.getDevices());
            // only update memory list,there is no need to update the disk file(already update by taskContainer)
            taskPool.runningTasks.remove(originTask);
        } else {
            // try to detect container status
            ContainerClient.ContainerStatus status = containerClient.status(newTask.getContainerId());
            switch (status) {
                case NORMAL:
                    // nothing to do
                    break;
                case DEAD:
                    // retry but with serial times
                    if (originTask.getRetryRestartNum() >= agentProperties.getTask().getRetryRestartMaxNum()) {
                        log.error("task:{} maximum number of restart retries:{} has been reached, task failed",
                                originTask.getId(), agentProperties.getTask().getRetryRestartMaxNum());

                        recordLog(originTask,
                                String.format("stage:running, task:%s container is dead, maximum number of restart retries num has been reached, task failed", originTask.getId()), null);

                        sourcePool.release(newTask.getDevices());
                        newTask.setStatus(InferenceTaskStatus.FAIL);
                        taskPool.runningTasks.remove(originTask);
                        taskPool.failedTasks.add(newTask);
                    } else {
                        log.warn("container:{} is dead, now will restart it", originTask.getContainerId());

                        recordLog(originTask,
                                String.format("stage:running, task:%s container:%s is dead, now will restart it", originTask.getId(), originTask.getContainerId()), null);

                        originTask.retryRestart();
                        // this invokes must before restart
                        // logRecorder.restart(originTask.getId(), originTask.getContainerId());

                        containerClient.startContainer(originTask.getContainerId());

                    }
                    break;
                case NO_SUCH_CONTAINER:
                    // already be removed or any else error
                    log.error("container:{} may be removed, now will return error", newTask.getContainerId());

                    recordLog(originTask,
                            String.format("stage:running, task:%s container:%s not found, may be removed, now will return error", originTask.getId(), originTask.getContainerId()), null);

                    newTask.setStatus(InferenceTaskStatus.FAIL);
                    taskPool.failedTasks.add(newTask);
                    // if run success, release device to available device pool
                    sourcePool.release(newTask.getDevices());
                    // only update memory list,there is no need to update the disk file(already update by taskContainer)
                    taskPool.runningTasks.remove(originTask);
                    break;
            }
        }
    }
}
