/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.taskexecutor.Scheduler;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.taskexecutor.TaskExecutor;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskPool;
import ai.starwhale.mlops.agent.taskexecutor.initializer.TaskPoolInitializer;
import ai.starwhale.mlops.api.ReportApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskConfiguration {

    @Bean
    public TaskPool taskPool() {
        return new TaskPool();
    }

    @Bean
    public TaskExecutor agentTaskExecutor(
        AgentProperties agentProperties,
        ReportApi reportApi,
        ContainerClient containerClient, SourcePool sourcePool,
        TaskPool taskPool) {
        return new TaskExecutor(agentProperties, reportApi, containerClient, sourcePool, taskPool);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.task.scheduler.enabled", havingValue = "true", matchIfMissing = true)
    public Scheduler agentTaskScheduler(TaskExecutor agentTaskExecutor) {
        return new Scheduler(agentTaskExecutor);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.task.rebuild.enabled", havingValue = "true", matchIfMissing = true)
    public TaskPoolInitializer taskPoolInitializer(AgentProperties agentProperties, TaskPool taskPool) {
        return new TaskPoolInitializer(agentProperties, taskPool);
    }
}
