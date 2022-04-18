/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.Scheduler;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.agent.task.executor.TaskExecutor;
import ai.starwhale.mlops.agent.task.initializer.TaskPoolInitializer;
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
    public TaskExecutor agentTaskExecutor(
            SourcePool sourcePool,
            TaskPool taskPool,
            DoTransition<Void, List<EvaluationTask>> rebuildTasksAction,
            DoTransition<EvaluationTask, EvaluationTask> init2PreparingAction,
            DoTransition<EvaluationTask, EvaluationTask> preparing2RunningAction,
            DoTransition<EvaluationTask, EvaluationTask> preparing2CanceledAction,
            DoTransition<EvaluationTask, EvaluationTask> finishedOrCanceled2ArchivedAction,
            DoTransition<EvaluationTask, EvaluationTask> monitorRunningTaskAction,
            DoTransition<EvaluationTask, EvaluationTask> running2CanceledAction,
            DoTransition<EvaluationTask, EvaluationTask> uploading2FinishedAction,
            DoTransition<EvaluationTask, EvaluationTask> uploading2CanceledAction,
            DoTransition<ReportRequest, ReportResponse> reportAction) {
        return new TaskExecutor(sourcePool, taskPool,
                rebuildTasksAction,
                init2PreparingAction,
                preparing2RunningAction, preparing2CanceledAction,
                finishedOrCanceled2ArchivedAction,
                monitorRunningTaskAction,
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
