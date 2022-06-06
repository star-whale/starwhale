/*
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

package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.UniqueID;
import ai.starwhale.mlops.agent.node.gpu.GPUDetect;
import ai.starwhale.mlops.agent.node.gpu.GPUInfo;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.RuntimeManifest;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.agent.task.inferencetask.executor.TaskExecutor;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence.ExecuteStatus;
import ai.starwhale.mlops.api.ReportApi;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.api.protocol.report.resp.SWRunTime;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.TaskType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = StarWhaleAgentTestApplication.class)
@TestPropertySource(
        properties = {"sw.agent.task.rebuild.enabled=false", "sw.agent.task.scheduler.enabled=false",
                "sw.agent.node.sourcePool.init.enabled=false"},
        locations = "classpath:application-integrationtest.yaml")
public class TaskExecutorTest {

    @MockBean
    private ReportApi reportApi;

    @MockBean
    private ContainerClient containerClient;

    @MockBean
    private GPUDetect nvidiaDetect;

    @MockBean
    private TaskPersistence taskPersistence;

    @MockBean
    private UniqueID uniqueID;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    Action<Void, List<InferenceTask>> rebuildTasksAction;

    @Autowired
    private TaskPool taskPool;

    @Autowired
    private SourcePool sourcePool;

    void mockConfig() throws IOException {
        Mockito.when(containerClient.createAndStartContainer(any()))
                .thenReturn(Optional.of("0dbb121b-1c5a-3a75-8063-0e1620edefe5"));
        Mockito.when(containerClient.containerInfo(any())).thenReturn(ContainerClient.ContainerInfo.builder().logPath("log-path").build());
        Mockito.when(taskPersistence.getAllActiveTasks()).thenReturn(Optional.of(
                List.of(
                        InferenceTask.builder()
                                .id(1234567890L)
                                .taskType(TaskType.PPL)
                                .status(InferenceTaskStatus.PREPARING)
                                .deviceClass(Device.Clazz.GPU)
                                .deviceAmount(1)
                                .swRunTime(SWRunTime.builder().name("swrt-name").version("swrt-version").build())
                                .swModelPackage(SWModelPackage.builder().name("swmp-name").version("swmp-version").build())
                                .build(),
                        InferenceTask.builder()
                                .id(2234567890L)
                                .taskType(TaskType.PPL)
                                .status(InferenceTaskStatus.PREPARING)
                                .deviceClass(Device.Clazz.GPU)
                                .swRunTime(SWRunTime.builder().name("swrt-name2").version("swrt-version2").build())
                                .swModelPackage(SWModelPackage.builder().name("swmp-name2").version("swmp-version2").build())
                                .deviceAmount(1)
                                .build()
                ))
        );
        Mockito.when(taskPersistence.save(any())).thenReturn(true);
        Mockito.when(taskPersistence.runtimeManifest(any())).thenReturn(RuntimeManifest.builder().baseImage("base-image").build());
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
        Mockito.when(uniqueID.id()).thenReturn("123456");

    }

    @Test
    public void fullFlowTest() throws IOException {
        mockConfig();

        rebuildTasksAction.apply(Void.TYPE.cast(null), Context.builder().build());
        sourcePool.refresh();
        sourcePool.setToReady();
        // check rebuild state
        assertEquals(2, taskPool.preparingTasks.size());

        //Mockito.when(taskPersistence.preloadingSWMP(any())).thenReturn("basepath/swmp/testmodel/v1/");
        // do prepare test
        taskExecutor.dealPreparingTasks();
        // check execute result
        assertEquals(1, taskPool.preparingTasks.size());
        assertEquals(1, taskPool.runningTasks.size());

        // mockConfig
        InferenceTask runningTask = taskPool.runningTasks.iterator().next();
        Long id = runningTask.getId();
        // mock taskContainer already change status to uploading
        // Mockito.when(taskPersistence.getTaskById(id)).thenReturn(runningTask);

        Mockito.when(taskPersistence.status(id)).thenReturn(Optional.of(ExecuteStatus.success));
        // do monitor test
        taskExecutor.monitorRunningTasks();

        // check execute result
        assertEquals(0, taskPool.runningTasks.size());
        assertEquals(1, taskPool.uploadingTasks.size());
        int idleNum = 0;
        for (Device device : sourcePool.getDevices()) {
            if (device.getClazz() == Device.Clazz.GPU && device.getStatus() == Device.Status.idle) {
                idleNum++;
            }
        }
        assertEquals(1, idleNum);

        // mockConfig
        // Mockito.when(taskPersistence.uploadResult(any())).thenReturn(true);

        taskExecutor.uploadTaskResults();
        // check execute result
        assertEquals(0, taskPool.uploadingTasks.size());
        assertEquals(1, taskPool.succeedTasks.size());

        // mockConfig:mock controller report api
        Mockito.when(reportApi.report(any()))
                .thenReturn(
                        ResponseMessage.<ReportResponse>builder()
                                .code("success")
                                .data(ReportResponse.builder().tasksToRun(List.of(
                                        TaskTrigger.builder()
                                                .swrt(SWRunTime.builder().build())
                                                .taskType(TaskType.PPL)
                                                .deviceClass(Device.Clazz.GPU)
                                                .deviceAmount(1)
                                                .swdsBlocks(List.of())
                                                .swModelPackage(SWModelPackage.builder().build())
                                                .id(666666L)
                                                .build()
                                )).build())
                                .build()
                );

        // do report test
        taskExecutor.reportTasks();
        // check execute result
        assertEquals(0, taskPool.succeedTasks.size());
        //assertEquals(1, taskPool.archivedTasks.size());
        assertEquals(2, taskPool.preparingTasks.size());

    }
}
