/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.normal;

import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Uploading2FinishedAction extends AbsBaseTaskTransition {

    @Override
    public EvaluationTask processing(EvaluationTask oldTask, Context context) {
        EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
        // todo: upload result file to the storage
        newTask.getTask().setResultPaths(List.of(""));
        newTask.getTask().setStatus(TaskStatus.FINISHED);
        return newTask;
    }

    @Override
    public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
        taskPool.uploadingTasks.remove(oldTask);
        taskPool.finishedTasks.add(newTask);
    }
}
