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

package ai.starwhale.test.reporting;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.HotJobHolderImpl;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.system.agent.AgentCache;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.reporting.ReportProcessorImp;
import ai.starwhale.mlops.schedule.CommandingTasksAssurance;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import ai.starwhale.mlops.schedule.SimpleSWTaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * test for {@link ai.starwhale.mlops.reporting.ReportProcessorImp}
 */
public class ReportProcessorImpTest {

    CommandingTasksAssurance commandingTasksAssurance;

    SWTaskScheduler swTaskScheduler;

    TaskBoConverter taskBoConverter;
    AgentCache agentCache;
    ObjectMapper jsonMapper;
    TaskMapper taskMapper;
    HotJobHolder jobHolder;
    ReportRequest report = mockReport();
    Agent agentMock = Agent.builder().serialNumber(report.getNodeInfo().getSerialNumber()).id(1L)
        .build();

    public void mockComponent() {
        swTaskScheduler = new SimpleSWTaskScheduler();
        commandingTasksAssurance = new CommandingTasksAssurance(swTaskScheduler);
        jobHolder = new HotJobHolderImpl();
        jsonMapper = new ObjectMapper();
        taskBoConverter = mock(TaskBoConverter.class);

        agentCache = mock(AgentCache.class);
        when(agentCache.nodeReport(report.getNodeInfo())).thenReturn(agentMock);
        taskMapper = mock(TaskMapper.class);
    }


    /**
     * 1 running 2 success 3 to_cancel 4 assigning
     */
    @Test
    public void testReceiveWithCommanding() {
        mockComponent();
        ReportProcessorImp reportProcessorImp = new ReportProcessorImp(commandingTasksAssurance,
            swTaskScheduler, taskBoConverter, agentCache, jsonMapper, taskMapper, jobHolder);
        List<Task> tasks = mockTask();
        jobHolder.adopt(
            Job.builder()
                .id(1L)
                .jobRuntime(JobRuntime.builder().deviceClass(Clazz.CPU).deviceAmount(1).name("runtimeName").version("runtimeVersion").build())

                .steps(List.of(Step.builder().id(1L).tasks(tasks).build()))
                .build());
        swTaskScheduler.adoptTasks(jobHolder.tasksOfIds(List.of(5L)), Clazz.CPU);
        swTaskScheduler.adoptTasks(jobHolder.tasksOfIds(List.of(6L)), Clazz.GPU);

        commandingTasksAssurance.onTaskCommanding(
            jobHolder.tasksOfIds(List.of(4L)).stream().map(t -> new TaskCommand(
                CommandType.TRIGGER, t)).collect(Collectors.toList()),
            agentMock);
        ReportResponse reportResponse = reportProcessorImp.receive(report);
        List<Long> taskIdsToCancel = reportResponse.getTaskIdsToCancel();
        Assertions.assertEquals(0,taskIdsToCancel.size());
        List<TaskTrigger> tasksToRun = reportResponse.getTasksToRun();
        Assertions.assertEquals(1,tasksToRun.size());
        Assertions.assertEquals(4L,tasksToRun.get(0).getId());
        tasks.forEach(task -> {
            if(task.getId().equals(1L)){
                Assertions.assertEquals(TaskStatus.RUNNING,task.getStatus());
            }
            if(task.getId().equals(2L)){
                Assertions.assertEquals(TaskStatus.SUCCESS,task.getStatus());
            }
        });

    }

    @Test
    public void testReceive() {
        mockComponent();
        ReportProcessorImp reportProcessorImp = new ReportProcessorImp(commandingTasksAssurance,
            swTaskScheduler, taskBoConverter, agentCache, jsonMapper, taskMapper, jobHolder);
        List<Task> tasks = mockTask();
        jobHolder.adopt(
            Job.builder()
                .id(1L)
                .jobRuntime(JobRuntime.builder().deviceClass(Clazz.CPU).deviceAmount(1).name("runtimeName").version("runtimeVersion").build())
                .status(JobStatus.TO_CANCEL)
                .steps(List.of(Step.builder().id(1L).tasks(tasks).build()))
                .build());
        swTaskScheduler.adoptTasks(jobHolder.tasksOfIds(List.of(5L)), Clazz.CPU);
        swTaskScheduler.adoptTasks(jobHolder.tasksOfIds(List.of(6L)), Clazz.GPU);

        ReportResponse reportResponse = reportProcessorImp.receive(report);
        List<Long> taskIdsToCancel = reportResponse.getTaskIdsToCancel();
        Assertions.assertEquals(1,taskIdsToCancel.size());
        Assertions.assertEquals(3L,taskIdsToCancel.get(0));
        List<TaskTrigger> tasksToRun = reportResponse.getTasksToRun();
        Assertions.assertEquals(2,tasksToRun.size());
        Set<Long> triggeredIds = tasksToRun.parallelStream().map(TaskTrigger::getId)
            .collect(Collectors.toSet());
        Assertions.assertTrue(triggeredIds.contains(5L));
        Assertions.assertTrue(triggeredIds.contains(6L));
        tasks.forEach(task -> {
            if(task.getId().equals(1L)){
                Assertions.assertEquals(TaskStatus.RUNNING,task.getStatus());
            }
            if(task.getId().equals(2L)){
                Assertions.assertEquals(TaskStatus.SUCCESS,task.getStatus());
            }
        });

    }

    List<Task> mockTask() {
        AtomicLong atomicLong = new AtomicLong(0);
        List<Task> tasks = new LinkedList<>();
        tasks.add(
            mockTask(atomicLong, TaskStatus.PREPARING));
        tasks.add(
            mockTask(atomicLong, TaskStatus.RUNNING));
        tasks.add(
            mockTask(atomicLong, TaskStatus.TO_CANCEL));
        Task t4 = mockTask(atomicLong, TaskStatus.ASSIGNING);
        tasks.add(t4);
        when(taskBoConverter.toTaskTrigger(t4)).thenReturn(TaskTrigger.builder().id(t4.getId()).build());
        Task t5 = mockTask(atomicLong, TaskStatus.READY);
        when(taskBoConverter.toTaskTrigger(t5)).thenReturn(TaskTrigger.builder().id(t5.getId()).build());
        tasks.add(t5);
        Task t6 = mockTask(atomicLong, TaskStatus.READY);
        when(taskBoConverter.toTaskTrigger(t6)).thenReturn(TaskTrigger.builder().id(t6.getId()).build());
        tasks.add(t6);
        when(taskBoConverter.toTaskTrigger(List.of(t5,t6))).thenReturn(List.of(TaskTrigger.builder().id(t6.getId()).build(),TaskTrigger.builder().id(t5.getId()).build()));
        return tasks;
    }

    private ReportRequest mockReport() {
        return ReportRequest.builder()
            .nodeInfo(mockNode(3, 6))
            .tasks(List.of(
                    TaskReport.builder().id(1L).status(TaskStatusInterface.RUNNING).build()
                    , TaskReport.builder().id(2L).status(TaskStatusInterface.SUCCESS).build()
                    , TaskReport.builder().id(7L).status(TaskStatusInterface.PREPARING).build()
                )
            )
            .build();
    }


    /**
     * @param idleCPU <= 4
     * @param idleGPU <= 8
     * @return
     */
    Node mockNode(int idleCPU, int idleGPU) {
        return Node.builder().serialNumber(UUID.randomUUID().toString())
            .ipAddr("120.0.9.8")
            .devices(mockDevices(idleCPU, idleGPU))
            .build();
    }

    private List<Device> mockDevices(int idleCPU, int idleGPU) {
        List<Device> mockedDevices = new LinkedList<>();
        mockOneType(mockedDevices, Clazz.CPU, 4, idleCPU);
        mockOneType(mockedDevices, Clazz.GPU, 8, idleGPU);
        return mockedDevices;
    }

    private void mockOneType(List<Device> mockedDevices, Device.Clazz clazz, int deviceCount,
        int idleCount) {
        for (int i = 0; i < deviceCount; i++) {
            mockedDevices.add(mockDevice(clazz, i < idleCount ? Status.idle : Status.busy));
        }
    }

    private Device mockDevice(Device.Clazz deviceClass, Status deviceStatus) {
        return Device.builder().clazz(deviceClass)
            .status(deviceStatus)
            .build();
    }



    private Task mockTask(AtomicLong atomicLong, TaskStatus status) {
        return Task.builder()
            .id(atomicLong.incrementAndGet())
            .uuid(UUID.randomUUID().toString())
            .status(status)
            .build();
    }
}
