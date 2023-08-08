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

import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.schedule.impl.docker.ContainerTaskMapper;
import ai.starwhale.mlops.schedule.impl.docker.DockerClientFinder;
import ai.starwhale.mlops.schedule.impl.docker.SwTaskSchedulerDocker;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DockerTaskReporterTest {

    TaskReportReceiver taskReportReceiver;
    SystemSettingService systemSettingService;
    DockerClientFinder dockerClientFinder;
    ContainerTaskMapper containerTaskMapper;
    ContainerStatusExplainer containerStatusExplainer;
    TaskStatusMachine taskStatusMachine;
    DockerTaskReporter dockerTaskReporter;

    @BeforeEach
    public void setup() {
        taskReportReceiver = mock(TaskReportReceiver.class);
        systemSettingService = mock(SystemSettingService.class);
        dockerClientFinder = mock(DockerClientFinder.class);
        containerTaskMapper = mock(ContainerTaskMapper.class);
        containerStatusExplainer = mock(ContainerStatusExplainer.class);
        taskStatusMachine = mock(TaskStatusMachine.class);
        dockerTaskReporter = new DockerTaskReporter(taskReportReceiver, systemSettingService, dockerClientFinder,
                containerTaskMapper, containerStatusExplainer, taskStatusMachine);
    }

    @Test
    public void testReportTask() {
        Container c = mock(Container.class);
        when(c.getNames()).thenReturn(new String[]{"a"});
        when(containerTaskMapper.taskIfOfContainer(any())).thenReturn(1L);
        when(containerStatusExplainer.statusOf(c, 1L)).thenReturn(TaskStatus.CANCELED);
        when(taskStatusMachine.isFinal(any())).thenReturn(true);
        dockerTaskReporter.reportTask(c);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(taskReportReceiver).receive(captor.capture());
        List reportedTasks = captor.getValue();
        ReportedTask t = (ReportedTask) reportedTasks.get(0);
        Assertions.assertEquals(1L, t.getId());
        Assertions.assertEquals(TaskStatus.CANCELED, t.getStatus());
        Assertions.assertNull(t.getFailedReason());
        Assertions.assertNotNull(t.getStopTimeMillis());

        String failReason = "Exit (1) blab-la";
        when(c.getStatus()).thenReturn(failReason);
        when(containerStatusExplainer.statusOf(c, 1L)).thenReturn(TaskStatus.FAIL);
        when(taskStatusMachine.isFinal(any())).thenReturn(true);
        dockerTaskReporter.reportTask(c);
        verify(taskReportReceiver, times(2)).receive(captor.capture());
        reportedTasks = captor.getValue();
        t = (ReportedTask) reportedTasks.get(0);
        Assertions.assertEquals(1L, t.getId());
        Assertions.assertEquals(TaskStatus.FAIL, t.getStatus());
        Assertions.assertEquals(failReason, t.getFailedReason());
        Assertions.assertNotNull(t.getStopTimeMillis());

    }

    @Test
    public void testReportTasks(){
        when(systemSettingService.getAllResourcePools()).thenReturn(List.of(new ResourcePool(), new ResourcePool()));
        doTest();
    }

    @Test
    public void testReportTasksWithNoResourcePool(){
        when(systemSettingService.getAllResourcePools()).thenReturn(null);
        doTest();
    }

    private void doTest() {
        DockerClient dockerClient = mock(DockerClient.class);
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(dockerClient);
        ListContainersCmd listContainersCmd = mock(
                ListContainersCmd.class);
        when(listContainersCmd.withLabelFilter(SwTaskSchedulerDocker.CONTAINER_LABELS)).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        Container c = mock(Container.class);
        when(listContainersCmd.exec()).thenReturn(List.of(c));
        when(c.getNames()).thenReturn(new String[]{"a"});
        when(containerTaskMapper.taskIfOfContainer(any())).thenReturn(1L);
        when(containerStatusExplainer.statusOf(c, 1L)).thenReturn(TaskStatus.CANCELED);
        when(taskStatusMachine.isFinal(any())).thenReturn(true);

        dockerTaskReporter.reportTasks();
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(taskReportReceiver).receive(captor.capture());
        Assertions.assertEquals(1, captor.getValue().size());
    }

}
