/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.agent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * run multiple AgentMockers
 */
public class AgentMockerRunner {

    int agentNumber = 3;

    int startIp = 50;

    static final String FORMATTER_IP="129.1.3.%d";

    ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(agentNumber);

    @Test
    public void testMultipleAgent() throws InterruptedException {
        for(int i=0;i<=agentNumber;i++){
            final int tmpi=i;
            WORKER_THREAD_POOL.submit(()->{
                try {
                    new AgentMocker(String.format(FORMATTER_IP,startIp + tmpi)).start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        WORKER_THREAD_POOL.awaitTermination(30,TimeUnit.MINUTES);

    }

}
