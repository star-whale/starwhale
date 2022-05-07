/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.LogRecorder;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.agent.task.inferencetask.TaskScheduler;
import ai.starwhale.mlops.agent.task.inferencetask.executor.TaskExecutor;
import ai.starwhale.mlops.agent.task.inferencetask.initializer.TaskPoolInitializer;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.FileSystemPath;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
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
    public LogRecorder logRecorder(ContainerClient containerClient, TaskPersistence taskPersistence) {
        return new LogRecorder(containerClient, taskPersistence);
    }

    @Bean
    public FileSystemPath fileSystemPath(AgentProperties agentProperties) {
        return new FileSystemPath(agentProperties.getBasePath());
    }

    @Bean
    public TaskExecutor agentTaskExecutor(
            SourcePool sourcePool,
            TaskPool taskPool,
            Action<Void, List<InferenceTask>> rebuildTasksAction,
            Action<InferenceTask, InferenceTask> init2PreparingAction,
            Action<InferenceTask, InferenceTask> preparing2RunningAction,
            Action<InferenceTask, InferenceTask> preparing2CanceledAction,
            Action<InferenceTask, InferenceTask> archivedAction,
            Action<InferenceTask, InferenceTask> monitoringAction,
            Action<InferenceTask, InferenceTask> running2CanceledAction,
            Action<InferenceTask, InferenceTask> uploading2FinishedAction,
            Action<InferenceTask, InferenceTask> uploading2CanceledAction,
            Action<ReportRequest, ReportResponse> reportAction) {
        return new TaskExecutor(sourcePool, taskPool,
                rebuildTasksAction,
                init2PreparingAction,
                preparing2RunningAction, preparing2CanceledAction,
                archivedAction,
                monitoringAction,
                running2CanceledAction,
                uploading2FinishedAction, uploading2CanceledAction,
                reportAction);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.agent.task.scheduler.enabled", havingValue = "true", matchIfMissing = true)
    public TaskScheduler agentTaskScheduler(TaskExecutor agentTaskExecutor, LogRecorder logRecorder) {
        return new TaskScheduler(agentTaskExecutor, logRecorder);
    }

}
