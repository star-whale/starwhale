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

package ai.starwhale.mlops.schedule.impl.docker.reporting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.schedule.impl.docker.ContainerRunMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.impl.docker.RunExecutorDockerImpl;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DockerExecutorReporterTest {

    RunReportReceiver runReportReceiver;
    SystemSettingService systemSettingService;
    DockerClientFinder dockerClientFinder;
    ContainerRunMapper containerRunMapper;
    ContainerStatusExplainer containerStatusExplainerOnLabel;
    DockerExecutorReporter dockerExecutorReporter;

    @BeforeEach
    public void setup() {
        runReportReceiver = mock(RunReportReceiver.class);
        systemSettingService = mock(SystemSettingService.class);
        dockerClientFinder = mock(DockerClientFinder.class);
        containerRunMapper = mock(ContainerRunMapper.class);
        containerStatusExplainerOnLabel = mock(ContainerStatusExplainer.class);
        dockerExecutorReporter = new DockerExecutorReporter(runReportReceiver, systemSettingService, dockerClientFinder,
                                                            containerRunMapper, containerStatusExplainerOnLabel);
    }

    @Test
    public void testReportRun() {
        Container c = mock(Container.class);
        when(c.getNames()).thenReturn(new String[]{"a"});
        when(containerRunMapper.runIdOfContainer(any())).thenReturn(1L);
        when(containerStatusExplainerOnLabel.statusOf(c)).thenReturn(RunStatus.FINISHED);
        dockerExecutorReporter.reportRun(c);
        ArgumentCaptor<ReportedRun> captor = ArgumentCaptor.forClass(ReportedRun.class);
        verify(runReportReceiver).receive(captor.capture());
        ReportedRun reportedRun = captor.getValue();
        Assertions.assertEquals(1L, reportedRun.getId());
        Assertions.assertEquals(RunStatus.FINISHED, reportedRun.getStatus());
        Assertions.assertNull(reportedRun.getFailedReason());
        Assertions.assertNotNull(reportedRun.getStopTimeMillis());

        String failReason = "Exit (1) blab-la";
        when(c.getStatus()).thenReturn(failReason);
        when(containerStatusExplainerOnLabel.statusOf(c)).thenReturn(RunStatus.FAILED);
        dockerExecutorReporter.reportRun(c);
        verify(runReportReceiver, times(2)).receive(captor.capture());
        reportedRun = captor.getValue();
        Assertions.assertEquals(1L, reportedRun.getId());
        Assertions.assertEquals(RunStatus.FAILED, reportedRun.getStatus());
        Assertions.assertEquals(failReason, reportedRun.getFailedReason());
        Assertions.assertNotNull(reportedRun.getStopTimeMillis());

    }

    @Test
    public void testReportTasks() {
        when(systemSettingService.getAllResourcePools()).thenReturn(List.of(new ResourcePool(), new ResourcePool()));
        doTest();
    }

    @Test
    public void testReportTasksWithNoResourcePool() {
        when(systemSettingService.getAllResourcePools()).thenReturn(null);
        doTest();
    }

    private void doTest() {
        DockerClient dockerClient = mock(DockerClient.class);
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(dockerClient);
        ListContainersCmd listContainersCmd = mock(
                ListContainersCmd.class);
        when(listContainersCmd.withLabelFilter(RunExecutorDockerImpl.CONTAINER_LABELS)).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        Container c = mock(Container.class);
        when(listContainersCmd.exec()).thenReturn(List.of(c));
        when(c.getNames()).thenReturn(new String[]{"a"});
        when(containerRunMapper.runIdOfContainer(any())).thenReturn(1L);
        when(containerStatusExplainerOnLabel.statusOf(c)).thenReturn(RunStatus.FAILED);

        dockerExecutorReporter.reportRuns();
        verify(runReportReceiver, times(1)).receive(any());
    }

}
