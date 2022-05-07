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
