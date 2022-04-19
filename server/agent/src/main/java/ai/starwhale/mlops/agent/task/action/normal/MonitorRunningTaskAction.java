/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.normal;

import ai.starwhale.mlops.agent.task.PPLTask;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.persistence.TaskPersistence.ExecuteStatus;
import ai.starwhale.mlops.domain.task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MonitorRunningTaskAction extends AbsBaseTaskTransition {

    @Override
    public PPLTask processing(PPLTask runningTask, Context context)
        throws Exception {
        // dominated by disk(see if other processes have modified)
        PPLTask newTask = BeanUtil.toBean(runningTask, PPLTask.class);

        Optional<ExecuteStatus> executeStatus = taskPersistence.status(runningTask.getId());
        if (executeStatus.isPresent()) {
            switch (executeStatus.get()) {
                case start:
                case running:
                case unknown:
                    // nothing to do,just wait
                    break;
                case success:
                    newTask.setStatus(TaskStatus.UPLOADING);
                    break;
                case failed:
                    newTask.setStatus(TaskStatus.EXIT_ERROR);
                    break;
            }
        }
        return newTask;
    }

    @Override
    public void success(PPLTask oldTask, PPLTask newTask, Context context) {

        if (newTask.getStatus() == TaskStatus.UPLOADING) {
            taskPool.uploadingTasks.add(newTask);
            // if run success, release device to available device pool todo:is there anything else to do?
            sourcePool.release(newTask.getDevices());
            // only update memory list,there is no need to update the disk file(already update by taskContainer)
            taskPool.runningTasks.remove(oldTask);
        } else if (newTask.getStatus() == TaskStatus.EXIT_ERROR) {
            taskPool.errorTasks.add(newTask);
            // if run success, release device to available device pool todo:is there anything else to do?
            sourcePool.release(newTask.getDevices());
            // only update memory list,there is no need to update the disk file(already update by taskContainer)
            taskPool.runningTasks.remove(oldTask);
        } else {
            // seem like no other status
        }
    }

}
