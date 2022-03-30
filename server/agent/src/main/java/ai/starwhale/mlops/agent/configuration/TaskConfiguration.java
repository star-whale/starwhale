/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskConfiguration {

   /* @Bean
    public TaskPool taskPool() {
        return new TaskPool();
    }

    @Bean
    public TaskExecutor agentTaskExecutor() {
        return new TaskExecutor();
    }

    @Bean
    public TaskPersistence fileSystemTaskPersistence(AgentProperties agentProperties) {
        return new FileSystemTaskPersistence(agentProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.task.scheduler.enabled", havingValue = "true", matchIfMissing = true)
    public Scheduler agentTaskScheduler(TaskExecutor agentTaskExecutor) {
        return new Scheduler(agentTaskExecutor);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.task.rebuild.enabled", havingValue = "true", matchIfMissing = true)
    public TaskPoolInitializer taskPoolInitializer() {
        return new TaskPoolInitializer();
    }*/
}
