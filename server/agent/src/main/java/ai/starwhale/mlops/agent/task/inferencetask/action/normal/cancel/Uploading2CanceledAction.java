/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal.cancel;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.Context;
import org.springframework.stereotype.Service;

@Service
public class Uploading2CanceledAction extends AbsBaseCancelPPLTaskAction {
    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {
        taskPool.uploadingTasks.remove(oldTask);
        super.success(oldTask, newTask, context);
    }
}
