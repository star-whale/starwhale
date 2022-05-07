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

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask.ActionStatus;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.FileSystemPath;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbsBasePPLTaskAction implements Action<InferenceTask, InferenceTask> {

    @Autowired
    protected TaskPersistence taskPersistence;

    @Autowired
    protected FileSystemPath fileSystemPath;

    @Autowired
    protected TaskPool taskPool;

    @Autowired
    protected SourcePool sourcePool;

    @Autowired
    protected ContainerClient containerClient;

    @Autowired
    protected AgentProperties agentProperties;

    @Override
    public void pre(InferenceTask task, Context context) {
        task.setActionStatus(ActionStatus.inProgress);
        taskPersistence.save(task);
    }

    @Override
    public void post(InferenceTask oldTask, InferenceTask newTask, Context context) {
        newTask.setActionStatus(ActionStatus.completed);
        taskPersistence.save(newTask);
    }

    @Override
    public void fail(InferenceTask task, Context context, Exception e) {
        log.error("execute task:{}, error:{}", JSONUtil.toJsonStr(task), e.getMessage(), e);
    }
}
