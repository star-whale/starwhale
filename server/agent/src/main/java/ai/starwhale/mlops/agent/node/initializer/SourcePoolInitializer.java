/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.initializer;

import ai.starwhale.mlops.agent.node.SourcePool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

/**
 * execute on every startup
 */
@Slf4j
@Order(0)
public class SourcePoolInitializer implements CommandLineRunner {
    @Autowired
    private SourcePool sourcePool;

    @Override
    public void run(String... args) throws Exception {
        sourcePool.refresh();
        sourcePool.setToReady();
    }
}
