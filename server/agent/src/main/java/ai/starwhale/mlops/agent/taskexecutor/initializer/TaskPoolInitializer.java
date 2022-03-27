/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor.initializer;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction.Context;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskPool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * execute on every startup
 */
@Slf4j
public class TaskPoolInitializer implements CommandLineRunner {

    private final AgentProperties agentProperties;

    private final TaskPool taskPool;

    public TaskPoolInitializer(AgentProperties agentProperties,
        TaskPool taskPool) {
        this.agentProperties = agentProperties;
        this.taskPool = taskPool;
    }

    @Override
    public void run(String... args) throws Exception {
        TaskAction.rebuildTasks.apply(agentProperties.getTask().getInfoPath(),
            Context.builder().taskPool(taskPool).build());
    }
}
