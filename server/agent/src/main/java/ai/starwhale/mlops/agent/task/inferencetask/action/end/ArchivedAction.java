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

package ai.starwhale.mlops.agent.task.inferencetask.action.end;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceStage;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.action.normal.AbsBaseTaskAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ArchivedAction extends AbsBaseTaskAction {

    @Override
    public InferenceTask processing(InferenceTask originTask, Context context)
        throws Exception {
        originTask.setStatus(InferenceTaskStatus.ARCHIVED);
        return originTask;
    }

    @Override
    public void success(InferenceTask originTask, InferenceTask newTask, Context context) throws Exception {
        // upload log
        if (StringUtils.isNotEmpty(originTask.getContainerId())) {
            try {
                ContainerClient.ContainerInfo info = containerClient.containerInfo(originTask.getContainerId());
                taskPersistence.uploadContainerLog(originTask, info.getLogPath());
                // remove container
                containerClient.removeContainer(originTask.getContainerId(), true);
            } catch (Exception e) {
                log.error("occur some error when upload container log:{}", e.getMessage(), e);
            }

        }
        // upload agent log to the storage
        taskPersistence.uploadLog(originTask);
        // remove from origin list
        taskPool.failedTasks.remove(originTask);
        taskPool.succeedTasks.remove(originTask);
        taskPool.canceledTasks.remove(originTask);

        // todo logRecorder.remove(originTask.getId());

    }

    @Override
    public InferenceStage stage() {
        return InferenceStage.ARCHIVED;
    }
}
