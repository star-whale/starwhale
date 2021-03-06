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

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceStage;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import org.springframework.stereotype.Service;

@Service
public class Uploading2FinishedAction extends AbsBaseTaskAction {

    @Override
    public InferenceTask processing(InferenceTask originTask, Context context) throws Exception {
        // upload result file to the storage
        taskPersistence.uploadResult(originTask);
        
        originTask.setStatus(InferenceTaskStatus.SUCCESS);
        return originTask;

    }

    @Override
    public void success(InferenceTask originTask, InferenceTask newTask, Context context) {
        taskPool.uploadingTasks.remove(originTask);
        taskPool.succeedTasks.add(newTask);
    }

    @Override
    public InferenceStage stage() {
        return InferenceStage.UPLOADING2FINISHED;
    }
}
