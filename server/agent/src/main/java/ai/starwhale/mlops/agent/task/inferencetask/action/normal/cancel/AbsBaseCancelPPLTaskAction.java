/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
