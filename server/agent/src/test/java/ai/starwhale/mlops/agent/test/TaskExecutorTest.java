/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.gpu.GPUInfo;
import ai.starwhale.mlops.agent.node.gpu.NvidiaDetect;
import ai.starwhale.mlops.agent.report.ReportHttpClient;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.agent.task.executor.TaskExecutor;
import ai.starwhale.mlops.agent.task.persistence.TaskPersistence;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

@SpringBootTest(
        classes = StarWhaleAgentTestApplication.class)
@TestPropertySource(
        properties = {"sw.task.rebuild.enabled=false", "sw.task.scheduler.enabled=false", "sw.node.sourcePool.init.enabled=false"},
        locations = "classpath:application-integrationtest.yaml")
public class TaskExecutorTest {
    @Autowired
    private AgentProperties agentProperties;

    @MockBean
    private ReportHttpClient reportHttpClient;

    @MockBean
    private ContainerClient containerClient;

    // todo Have some problem:mock not effect
    @MockBean
    private NvidiaDetect nvidiaDetect;

    @MockBean
    private TaskPersistence taskPersistence;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    DoTransition<String, List<EvaluationTask>> rebuildTasksAction;

    @Autowired
    private TaskPool taskPool;

    @Autowired
    private SourcePool sourcePool;

    void mockConfig() throws IOException {
        Mockito.when(containerClient.startContainer(any(), any())).thenReturn(Optional.of("0dbb121b-1c5a-3a75-8063-0e1620edefe5"));
        Mockito.when(taskPersistence.getAll()).thenReturn(List.of(
            EvaluationTask.builder()
                .task(
                    Task.builder().id(1234567890L).jobId(222222L).status(TaskStatus.PREPARING).build()
                )
                .build(),
            EvaluationTask.builder()
                .task(
                    Task.builder().id(2234567890L).jobId(222222L).status(TaskStatus.PREPARING).build()
                )
                .build()
        ));
        Mockito.when(taskPersistence.save(any())).thenReturn(true);
        Mockito.when(nvidiaDetect.detect()).thenReturn(Optional.of(
                List.of(
                        GPUInfo.builder()
                                .id("1dbb121b-1c5a-3a75-8063-0e1620edefe6")
                                .driverInfo("driver:1.450.8, CUDA:10.1")
                                .brand("xxxx T4").name("swtest")
                                .processInfos(
                                        List.of(GPUInfo.ProcessInfo.builder().pid("1").build())
                                )
                                .build(),
                        GPUInfo.builder()
                                .id("2dbb121b-1c5a-3a75-8063-0e1620edefe8")
                                .driverInfo("driver:1.450.8, CUDA:10.1")
                                .brand("xxxx T4")
                                .name("swtest")
                                .build()
                )
        ));

    }

    @Test
    public void rebuild_preparing2RunningTest() throws IOException {
        mockConfig();

        URL taskPathUrl = ResourceUtils.getURL("classpath:tasks");
        rebuildTasksAction.apply(taskPathUrl.getPath().substring(1), Context.builder().build());
        sourcePool.refresh();
        sourcePool.setToReady();
        // check rebuild state
        assertEquals(2, taskPool.preparingTasks.size());
        // do prepare test
        taskExecutor.dealPreparingTasks();
        // check execute result
        assertEquals(1, taskPool.preparingTasks.size());
        assertEquals(1, taskPool.runningTasks.size());


        // mockConfig
        Mockito.when(taskPersistence.getTaskById(any())).thenReturn();
        taskExecutor.monitorRunningTasks();


    }
}
