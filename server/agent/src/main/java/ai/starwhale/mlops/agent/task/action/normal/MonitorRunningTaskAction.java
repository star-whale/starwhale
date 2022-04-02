/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.normal;

import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.domain.task.TaskStatus;
import java.io.IOException;
import java.util.Optional;
import ai.starwhale.mlops.agent.task.persistence.TaskPersistence.ExecuteStatus;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.stereotype.Service;

@Service
public class MonitorRunningTaskAction extends AbsBaseTaskTransition {

    @Override
    public EvaluationTask processing(EvaluationTask runningTask, Context context)
        throws Exception {
        // dominated by disk(see if other processes have modified)
        EvaluationTask newTask = BeanUtil.toBean(runningTask, EvaluationTask.class);

        ExecuteStatus executeStatus = taskPersistence.status(runningTask.getId());
        switch (executeStatus) {
            case START:
            case RUNNING:
            case UNKNOWN:
                // nothing to do,just wait
                break;
            case OK:
                newTask.setStatus(TaskStatus.UPLOADING);
                break;
            case FAILED:
                newTask.setStatus(TaskStatus.EXIT_ERROR);
                break;
        }

        return newTask;
    }

    @Override
    public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {

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

    @Override
    public void fail(EvaluationTask oldTask, Context context, Exception e) {
        // nothing
    }
}
