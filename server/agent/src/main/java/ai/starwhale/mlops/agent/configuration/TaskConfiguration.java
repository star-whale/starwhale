/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.ppltask.PPLTask;
import ai.starwhale.mlops.agent.task.ppltask.Scheduler;
import ai.starwhale.mlops.agent.task.ppltask.TaskPool;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.ppltask.executor.TaskExecutor;
import ai.starwhale.mlops.agent.task.ppltask.initializer.TaskPoolInitializer;
import ai.starwhale.mlops.agent.task.ppltask.persistence.FileSystemPath;
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TaskConfiguration {

    @Bean
    @ConditionalOnProperty(name = "sw.agent.task.rebuild.enabled", havingValue = "true", matchIfMissing = true)
    public TaskPoolInitializer taskPoolInitializer() {
        return new TaskPoolInitializer();
    }

    @Bean
    public TaskPool taskPool() {
        return new TaskPool();
    }

    @Bean
    public FileSystemPath fileSystemPath(AgentProperties agentProperties) {
        return new FileSystemPath(agentProperties.getBasePath());
    }

    @Bean
    public TaskExecutor agentTaskExecutor(
            SourcePool sourcePool,
            TaskPool taskPool,
            Action<Void, List<PPLTask>> rebuildTasksAction,
            Action<PPLTask, PPLTask> init2PreparingAction,
            Action<PPLTask, PPLTask> preparing2RunningAction,
            Action<PPLTask, PPLTask> preparing2CanceledAction,
            Action<PPLTask, PPLTask> finishedOrCanceled2ArchivedAction,
            Action<PPLTask, PPLTask> monitoringAction,
            Action<PPLTask, PPLTask> running2CanceledAction,
            Action<PPLTask, PPLTask> uploading2FinishedAction,
            Action<PPLTask, PPLTask> uploading2CanceledAction,
            Action<ReportRequest, ReportResponse> reportAction) {
        return new TaskExecutor(sourcePool, taskPool,
                rebuildTasksAction,
                init2PreparingAction,
                preparing2RunningAction, preparing2CanceledAction,
                finishedOrCanceled2ArchivedAction,
                monitoringAction,
                running2CanceledAction,
                uploading2FinishedAction, uploading2CanceledAction,
                reportAction);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.agent.task.scheduler.enabled", havingValue = "true", matchIfMissing = true)
    public Scheduler agentTaskScheduler(TaskExecutor agentTaskExecutor) {
        return new Scheduler(agentTaskExecutor);
    }

}
