/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal.cancel;

import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceStage;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class Running2CanceledAction extends AbsBaseCancelPPLTaskAction {
    @Override
    public Optional<InferenceStage> stage() {
        return Optional.of(InferenceStage.RUNNING);
    }

    @Override
    public InferenceTask processing(InferenceTask oldTask, Context context) {
        // stop the container
        if (containerClient.stopContainer(oldTask.getContainerId())) {
            return super.processing(oldTask, context);
        }
        return null;
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {
        if (Objects.nonNull(newTask)) {
            taskPool.runningTasks.remove(oldTask);
            super.success(oldTask, newTask, context);
        }
    }
}
