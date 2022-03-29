/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.cancel;

import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.normal.AbsBaseTaskTransition;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbsBaseCancelTaskTransition extends AbsBaseTaskTransition {
    @Autowired
    protected TaskPool taskPool;

    @Override
    public boolean valid(EvaluationTask evaluationTask, Context context) {
        return taskPool.needToCancel.contains(evaluationTask.getTask().getId());
    }

    @Override
    public EvaluationTask processing(EvaluationTask oldTask, Context context) {
        EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
        newTask.getTask().setStatus(TaskStatus.CANCELED);
        return newTask;
    }

    @Override
    public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
        taskPool.canceledTasks.add(newTask);
        // cancel success
        taskPool.needToCancel.remove(newTask.getTask().getId());
    }
}
