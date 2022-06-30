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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

@Slf4j
public abstract class AbsBaseTaskAction implements Action<InferenceTask, InferenceTask> {

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

    // at normal action, the newTask don't use at post
    @Override
    public void post(InferenceTask originTask, InferenceTask newTask, Context context) {
        originTask.setActionStatus(ActionStatus.completed);
        taskPersistence.save(originTask);
    }

    protected void recordLog(InferenceTask task, String simpleMsg, Exception e) {
        taskPersistence.recordLog(task, simpleMsg + ":" + getStackTrace(e));
    }

    private String getStackTrace(Throwable throwable) {
        if (Objects.isNull(throwable)) return "";
        StringWriter sw = new StringWriter();

        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }
}
