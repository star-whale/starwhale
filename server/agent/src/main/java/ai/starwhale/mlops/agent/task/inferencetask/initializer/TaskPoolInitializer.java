/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.initializer;

import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * execute on every startup
 */
@Slf4j
@Order(1)
public class TaskPoolInitializer implements CommandLineRunner {
    @Autowired
    private Action<Void, List<InferenceTask>> rebuildTasksAction;

    @Override
    public void run(String... args) throws Exception {
        rebuildTasksAction.apply(Void.TYPE.cast(null),
            Context.builder().build());
    }
}
