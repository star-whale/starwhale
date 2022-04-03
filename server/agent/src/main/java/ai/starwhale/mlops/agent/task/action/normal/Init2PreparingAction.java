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
import cn.hutool.core.bean.BeanUtil;
import org.springframework.stereotype.Service;

@Service
public class Init2PreparingAction extends AbsBaseTaskTransition {

    @Override
    public boolean valid(EvaluationTask obj, Context context) {
        return obj.getStatus() == TaskStatus.CREATED
            || obj.getStatus() == TaskStatus.ASSIGNING;
    }

    @Override
    public EvaluationTask processing(EvaluationTask oldTask, Context context) throws Exception {
        EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
        newTask.setStatus(TaskStatus.PREPARING);
        return newTask;
    }

    @Override
    public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
        // add the new task to the tail
        taskPool.preparingTasks.offer(newTask);
    }

    @Override
    public void fail(EvaluationTask evaluationTask, Context context, Exception e) {
        // nothing to do
    }
}
