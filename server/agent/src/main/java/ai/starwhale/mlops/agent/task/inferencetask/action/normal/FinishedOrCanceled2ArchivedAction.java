/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.domain.task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.stereotype.Service;

@Service
public class FinishedOrCanceled2ArchivedAction extends AbsBasePPLTaskAction {

    @Override
    public InferenceTask processing(InferenceTask oldTask, Context context)
        throws Exception {
        InferenceTask newTask = BeanUtil.toBean(oldTask, InferenceTask.class);
        // move to the archived dir
        taskPersistence.move2Archived(newTask);
        newTask.setStatus(TaskStatus.ARCHIVED);
        return newTask;
    }
    @Override
    public void post(InferenceTask oldTask, InferenceTask newTask, Context context) throws Exception {
        // just override super method
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {
        // remove from origin list
        taskPool.finishedTasks.remove(oldTask);
        taskPool.canceledTasks.remove(oldTask);
        taskPool.archivedTasks.add(newTask);
    }
}
