/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.initializer;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * execute on every startup
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sw.task.rebuild.enabled", havingValue = "true", matchIfMissing = true)
public class TaskPoolInitializer implements CommandLineRunner {
    @Autowired
    private AgentProperties agentProperties;
    @Autowired
    private TaskPool taskPool;
    @Autowired
    private DoTransition<String, List<EvaluationTask>> rebuildTasksAction;

    @Override
    public void run(String... args) throws Exception {
        rebuildTasksAction.apply(agentProperties.getTask().getInfoPath(),
            Context.builder().build());
    }
}
