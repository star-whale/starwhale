/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.end;

import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.LogRecorder;
import ai.starwhale.mlops.agent.task.inferencetask.action.normal.AbsBasePPLTaskAction;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArchivedAction extends AbsBasePPLTaskAction {

    @Autowired
    private LogRecorder logRecorder;

    @Override
    public InferenceTask processing(InferenceTask oldTask, Context context)
        throws Exception {
        InferenceTask newTask = BeanUtil.toBean(oldTask, InferenceTask.class);
        newTask.setStatus(InferenceTaskStatus.ARCHIVED);
        return newTask;
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) throws Exception {
        // remove from origin list
        taskPool.failedTasks.remove(oldTask);
        taskPool.succeedTasks.remove(oldTask);
        taskPool.canceledTasks.remove(oldTask);
        logRecorder.remove(oldTask.getId());
    }
}
