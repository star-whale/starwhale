/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal.cancel;

import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.agent.task.inferencetask.action.normal.AbsBasePPLTaskAction;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class AbsBaseCancelPPLTaskAction extends AbsBasePPLTaskAction implements ExecuteStage {
    @Autowired
    protected TaskPool taskPool;

    @Override
    public boolean valid(InferenceTask InferenceTask, Context context) {
        return taskPool.needToCancel.contains(InferenceTask.getId());
    }

    @Override
    public void pre(InferenceTask task, Context context) {
        task.setStage(stage().orElse(task.getStage()));
        task.setStatus(InferenceTaskStatus.CANCELING);
        super.pre(task, context);
    }

    @Override
    public InferenceTask processing(InferenceTask oldTask, Context context) {
        return BeanUtil.toBean(oldTask, InferenceTask.class);
    }

    @Override
    public void post(InferenceTask oldTask, InferenceTask newTask, Context context) {
        newTask.setStatus(InferenceTaskStatus.CANCELED);
        super.post(oldTask, newTask, context);
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {
        if (Objects.nonNull(newTask)) {
            taskPool.canceledTasks.add(newTask);
            // cancel success
            taskPool.needToCancel.remove(newTask.getId());
        }
    }
}
