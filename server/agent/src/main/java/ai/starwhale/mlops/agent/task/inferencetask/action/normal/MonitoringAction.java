/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence.ExecuteStatus;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class MonitoringAction extends AbsBasePPLTaskAction {

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
                    log.error("container:{} is dead, now will restart it", newTask.getContainerId());
                    containerClient.startContainer(newTask.getContainerId());
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
