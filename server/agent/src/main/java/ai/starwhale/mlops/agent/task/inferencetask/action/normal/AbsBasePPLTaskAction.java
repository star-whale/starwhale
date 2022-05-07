/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
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
