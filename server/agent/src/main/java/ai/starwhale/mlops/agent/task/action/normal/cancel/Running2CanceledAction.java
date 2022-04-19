/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.normal.cancel;

import ai.starwhale.mlops.agent.task.PPLTask;
import ai.starwhale.mlops.agent.task.action.Context;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class Running2CanceledAction extends AbsBaseCancelTaskTransition {
    @Override
    public PPLTask processing(PPLTask oldTask, Context context) {
        // stop the container
        if (containerClient.stopAndRemoveContainer(oldTask.getContainerId(), true)){
            return super.processing(oldTask, context);
        }
       return null;
    }

    @Override
    public void success(PPLTask oldTask, PPLTask newTask, Context context) {
        if (Objects.nonNull(newTask)) {
            taskPool.runningTasks.remove(oldTask);
            super.success(oldTask, newTask, context);
        }
    }
}
