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

package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.gpu.GPUDetect;
import ai.starwhale.mlops.agent.node.gpu.GPUInfo;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.agent.task.inferencetask.executor.TaskExecutor;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.FileSystemPath;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.api.protocol.report.resp.SWDSBlockVO;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swds.index.SWDSDataLocation;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.TaskType;
import cn.hutool.core.collection.CollectionUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = StarWhaleAgentTestApplication.class)
@TestPropertySource(
        properties = {
                "sw.agent.task.rebuild.enabled=false",
                "sw.agent.task.scheduler.enabled=false",
                "sw.agent.node.sourcePool.init.enabled=false",
                // when test,please set these properties with debug configuration
                //"sw.storage.s3-config.endpoint=http://${ip}:9000",
                "sw.agent.basePath=/var/starwhale"
        },
        locations = "classpath:application-integrationtest.yaml"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TaskActionTest {
    @MockBean
    private GPUDetect nvidiaDetect;
    @MockBean
    private ContainerClient containerClient;

    @Autowired
    private TaskPersistence taskPersistence;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    Action<Void, List<InferenceTask>> rebuildTasksAction;

    @Autowired
    Action<InferenceTask, InferenceTask> archivedAction;

    @Autowired
    private TaskPool taskPool;

    @Autowired
    private SourcePool sourcePool;

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    private FileSystemPath fileSystemPath;

    private void pre() throws IOException {
        if(Files.exists(Path.of(agentProperties.getBasePath()))) {
            // clear local dir
            FileUtils.cleanDirectory(new File(agentProperties.getBasePath()));
        } else {
            Files.createDirectory(Path.of(agentProperties.getBasePath()));
        }

    }

    @Test
    public void testPreparing2Running() throws IOException {
        pre();

        Mockito.when(containerClient.createAndStartContainer(any()))
                .thenReturn(Optional.of("0dbb121b-1c5a-3a75-8063-0e1620edefe5"));
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
        List<InferenceTask> tasks = List.of(
                InferenceTask.builder()
                        .id(1234567890L)
                        .status(InferenceTaskStatus.PREPARING)
                        .taskType(TaskType.PPL)
                        .deviceClass(Device.Clazz.GPU)
                        .deviceAmount(1)
                        .imageId("starwhaleai/starwhale:0.1.0-nightly-2022041203")
                        .swModelPackage(SWModelPackage.builder()
                                .id(1234567L)
                                .name("test-swmp")
                                .version("v1")
                                .path("StarWhale/controller/swmp/mnist/meytmy3dge4gcnrtmftdgyjzoazxg3y")
                                .build())
                        .swdsBlocks(List.of(
                                SWDSBlockVO.builder().id(123456L).locationInput(
                                        SWDSDataLocation.builder().file("test-data").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label").offset(100).size(100).build()
                                ).build(),
                            SWDSBlockVO.builder().id(123466L).locationInput(
                                        SWDSDataLocation.builder().file("test-data2").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label2").offset(100).size(100).build()
                                ).build()
                        ))
                        .resultPath(new ResultPath("todo"))
                        .build()
        );
        if (CollectionUtil.isNotEmpty(tasks)) {
            tasks.forEach(taskPool::fill);
            taskPool.setToReady();
        }
        sourcePool.refresh();
        sourcePool.setToReady();
        // do prepare test
        taskExecutor.dealPreparingTasks();
        // check execute result
        assertEquals(0, taskPool.preparingTasks.size());
        assertEquals(1, taskPool.runningTasks.size());
    }

    @Test
    public void testMonitorTask() throws Exception {
        pre();
        List<InferenceTask> tasks = List.of(
                InferenceTask.builder()
                        .id(1234567890L)
                        .status(InferenceTaskStatus.RUNNING) // change to runnning
                        .taskType(TaskType.PPL)
                        .containerId("test-containerid")
                        .deviceClass(Device.Clazz.GPU)
                        .deviceAmount(1)
                        .imageId("starwhaleai/starwhale:0.1.0-nightly-2022041203")
                        .swModelPackage(SWModelPackage.builder()
                                .id(1234567L)
                                .name("test-swmp")
                                .version("v1")
                                .path("StarWhale/controller/swmp/mnist/meytmy3dge4gcnrtmftdgyjzoazxg3y")
                                .build())
                        .swdsBlocks(List.of(
                            SWDSBlockVO.builder().id(123456L).locationInput(
                                        SWDSDataLocation.builder().file("test-data").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label").offset(100).size(100).build()
                                ).build(),
                            SWDSBlockVO.builder().id(123466L).locationInput(
                                        SWDSDataLocation.builder().file("test-data2").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label2").offset(100).size(100).build()
                                ).build()
                        ))
                        .resultPath(new ResultPath("todo"))
                        .build(),
                InferenceTask.builder()
                        .id(1234567891L)
                        .status(InferenceTaskStatus.RUNNING) // change to runnning
                        .taskType(TaskType.PPL)
                        .containerId("test-containerid2")
                        .deviceClass(Device.Clazz.GPU)
                        .deviceAmount(1)
                        .imageId("starwhaleai/starwhale:0.1.0-nightly-2022041203")
                        .swModelPackage(SWModelPackage.builder()
                                .id(1234567L)
                                .name("test-swmp")
                                .version("v1")
                                .path("StarWhale/controller/swmp/mnist/meytmy3dge4gcnrtmftdgyjzoazxg3y")
                                .build())
                        .swdsBlocks(List.of(
                            SWDSBlockVO.builder().id(123456L).locationInput(
                                        SWDSDataLocation.builder().file("test-data").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label").offset(100).size(100).build()
                                ).build(),
                            SWDSBlockVO.builder().id(123466L).locationInput(
                                        SWDSDataLocation.builder().file("test-data2").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label2").offset(100).size(100).build()
                                ).build()
                        ))
                        .resultPath(new ResultPath("todo"))
                        .build()
        );
        if (CollectionUtil.isNotEmpty(tasks)) {
            tasks.forEach(taskPool::fill);
            taskPool.setToReady();
        }

        Mockito.when(containerClient.status(any())).thenReturn(ContainerClient.ContainerStatus.NORMAL);
        // first to monitor
        taskExecutor.monitorRunningTasks();
        assertEquals(2, taskPool.runningTasks.size());

        // change status to ok
        taskPersistence.updateStatus(1234567890L, TaskPersistence.ExecuteStatus.success);
        // container has changed status to OK
        taskExecutor.monitorRunningTasks();

        assertEquals(1, taskPool.runningTasks.size());
        assertEquals(1, taskPool.uploadingTasks.size());
    }

    @Test
    public void testUpload() throws IOException {
        pre();

        List<InferenceTask> tasks = List.of(
                InferenceTask.builder()
                        .id(1234567890L)
                        .taskType(TaskType.PPL)
                        .status(InferenceTaskStatus.UPLOADING) // change to UPLOADING
                        .containerId("test-containerid")
                        .deviceClass(Device.Clazz.GPU)
                        .deviceAmount(1)
                        .imageId("starwhaleai/starwhale:0.1.0-nightly-2022041203")
                        .swModelPackage(SWModelPackage.builder()
                                .id(1234567L)
                                .name("test-swmp")
                                .version("v1")
                                .path("StarWhale/controller/swmp/mnist/meytmy3dge4gcnrtmftdgyjzoazxg3y")
                                .build())
                        .swdsBlocks(List.of(
                            SWDSBlockVO.builder().id(123456L).locationInput(
                                        SWDSDataLocation.builder().file("test-data").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label").offset(100).size(100).build()
                                ).build(),
                            SWDSBlockVO.builder().id(123466L).locationInput(
                                        SWDSDataLocation.builder().file("test-data2").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label2").offset(100).size(100).build()
                                ).build()
                        ))
                        .resultPath(new ResultPath("todo"))
                        .build(),
                InferenceTask.builder()
                        .id(1234567891L)
                        .taskType(TaskType.PPL)
                        .status(InferenceTaskStatus.UPLOADING) // change to UPLOADING
                        .containerId("test-containerid2")
                        .deviceClass(Device.Clazz.GPU)
                        .deviceAmount(1)
                        .imageId("starwhaleai/starwhale:0.1.0-nightly-2022041203")
                        .swModelPackage(SWModelPackage.builder()
                                .id(1234567L)
                                .name("test-swmp")
                                .version("v1")
                                .path("StarWhale/controller/swmp/mnist/meytmy3dge4gcnrtmftdgyjzoazxg3y")
                                .build())
                        .swdsBlocks(List.of(
                            SWDSBlockVO.builder().id(123456L).locationInput(
                                        SWDSDataLocation.builder().file("test-data").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label").offset(100).size(100).build()
                                ).build(),
                            SWDSBlockVO.builder().id(123466L).locationInput(
                                        SWDSDataLocation.builder().file("test-data2").offset(0).size(100).build()
                                ).locationLabel(
                                        SWDSDataLocation.builder().file("test-label2").offset(100).size(100).build()
                                ).build()
                        ))
                        .resultPath(new ResultPath("todo"))
                        .build()
        );
        if (CollectionUtil.isNotEmpty(tasks)) {
            tasks.forEach(taskPool::fill);
            taskPool.setToReady();
        }

        // first to monitor
        taskExecutor.uploadTaskResults();
        assertEquals(2, taskPool.uploadingTasks.size());

    }


    @Test
    public void testArchived() throws IOException {
        pre();
        InferenceTask task = InferenceTask.builder()
                .id(1234567890L)
                .containerId("container-1")
                .resultPath(new ResultPath("todo"))
                .build();
        Mockito.when(containerClient.containerInfo(any())).thenReturn(ContainerClient.ContainerInfo.builder().logPath("log-path").build());
        assertFalse(Files.exists(Path.of(fileSystemPath.oneArchivedTaskDir(task.getId()))));
        archivedAction.apply(task, null);
        assertTrue(Files.exists(Path.of(fileSystemPath.oneArchivedTaskDir(task.getId()))));
    }
}
