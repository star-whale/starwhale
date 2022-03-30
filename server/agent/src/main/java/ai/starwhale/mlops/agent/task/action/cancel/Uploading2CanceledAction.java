/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.cancel;

import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.action.Context;
import org.springframework.stereotype.Service;

@Service
public class Uploading2CanceledAction extends AbsBaseCancelTaskTransition {
    @Override
    public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
        taskPool.uploadingTasks.remove(oldTask);
    }
}
