/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.ppltask.action.normal.cancel;

import ai.starwhale.mlops.agent.task.ppltask.PPLTask;
import ai.starwhale.mlops.agent.task.ppltask.TaskPool;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.ppltask.action.normal.AbsBasePPLTaskAction;
import ai.starwhale.mlops.domain.task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class AbsBaseCancelPPLTaskAction extends AbsBasePPLTaskAction {
    @Autowired
    protected TaskPool taskPool;

    @Override
    public boolean valid(PPLTask PPLTask, Context context) {
        return taskPool.needToCancel.contains(PPLTask.getId());
    }

    @Override
    public PPLTask processing(PPLTask oldTask, Context context) {
        PPLTask newTask = BeanUtil.toBean(oldTask, PPLTask.class);
        newTask.setStatus(TaskStatus.CANCELED);
        return newTask;
    }

    @Override
    public void success(PPLTask oldTask, PPLTask newTask, Context context) {
        if (Objects.nonNull(newTask)) {
            taskPool.canceledTasks.add(newTask);
            // cancel success
            taskPool.needToCancel.remove(newTask.getId());
        }
    }
}
