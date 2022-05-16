/*
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
