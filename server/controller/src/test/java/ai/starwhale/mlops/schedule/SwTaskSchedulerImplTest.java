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

package ai.starwhale.mlops.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SwTaskSchedulerImplTest {

    RunExecutor runExecutor;
    RunReportReceiver runReportReceiver;
    TaskContainerSpecificationFinder taskContainerSpecificationFinder;

    RunMapper runMapper;

    SwTaskSchedulerImpl swTaskScheduler;

    Task task = Task.builder()
            .id(1L)
            .step(Step.builder()
                          .job(Job.builder().id(1L).build())
                          .resourcePool(ResourcePool.builder().name("rp").build())
                          .build())
            .taskRequest(TaskRequest.builder()
                                 .runtimeResources(List.of(RuntimeResource.builder().type("cpu").build()))
                                 .build())
            .resultRootPath(new ResultPath("/tmp"))
            .build();

    @BeforeEach
    void setup() {
        runExecutor = mock(RunExecutor.class);
        runReportReceiver = mock(RunReportReceiver.class);
        taskContainerSpecificationFinder = mock(TaskContainerSpecificationFinder.class);
        ContainerSpecification containerSpecification = mock(ContainerSpecification.class);
        when(taskContainerSpecificationFinder.findCs(task)).thenReturn(containerSpecification);
        when(containerSpecification.getContainerEnvs()).thenReturn(Map.of("aaa", "bbb"));
        when(containerSpecification.getImage()).thenReturn("img");
        when(containerSpecification.getCmd()).thenReturn(new ContainerCommand(
                new String[]{"cmd"},
                new String[]{"bash -c"}
        ));
        runMapper = mock(RunMapper.class);
        doAnswer(invocation -> {
            RunEntity runEntity = invocation.getArgument(0);
            runEntity.setId(11L);
            return null;
        }).when(runMapper).insert(any(RunEntity.class));
        swTaskScheduler = new SwTaskSchedulerImpl(
                runExecutor,
                runReportReceiver,
                taskContainerSpecificationFinder,
                runMapper
        );
    }

    @Test
    void schedule() {
        swTaskScheduler.schedule(task);
        ArgumentCaptor<Run> runArgumentCaptor = ArgumentCaptor.forClass(Run.class);
        verify(runExecutor).run(runArgumentCaptor.capture(), eq(runReportReceiver));
        Run run = runArgumentCaptor.getValue();
        Assertions.assertEquals(1L, run.getTaskId());
        Assertions.assertEquals(run, task.getCurrentRun());
        Assertions.assertEquals("bbb", run.getRunSpec().getEnvs().get("aaa"));
        Assertions.assertEquals("img", run.getRunSpec().getImage());
        Assertions.assertEquals("cmd", run.getRunSpec().getCommand().getCmd()[0]);
        Assertions.assertEquals("bash -c", run.getRunSpec().getCommand().getEntrypoint()[0]);
        Assertions.assertEquals("rp", run.getRunSpec().getResourcePool().getName());
        Assertions.assertEquals("cpu", run.getRunSpec().getRequestedResources().get(0).getType());
        Assertions.assertEquals("/tmp/logs", run.getLogDir());
        ArgumentCaptor<RunEntity> runEntityArgumentCaptor = ArgumentCaptor.forClass(RunEntity.class);
        verify(runMapper).insert(runEntityArgumentCaptor.capture());
        RunEntity runEntity = runEntityArgumentCaptor.getValue();
        Assertions.assertEquals(1L, runEntity.getTaskId());
        Assertions.assertEquals("bbb", runEntity.getRunSpec().getEnvs().get("aaa"));
        Assertions.assertEquals("img", runEntity.getRunSpec().getImage());
        Assertions.assertEquals("cmd", runEntity.getRunSpec().getCommand().getCmd()[0]);
        Assertions.assertEquals("bash -c", runEntity.getRunSpec().getCommand().getEntrypoint()[0]);
        Assertions.assertEquals("rp", runEntity.getRunSpec().getResourcePool().getName());
        Assertions.assertEquals("cpu", runEntity.getRunSpec().getRequestedResources().get(0).getType());
        Assertions.assertEquals("/tmp/logs", runEntity.getLogDir());
    }

    @Test
    void stop() {
        swTaskScheduler.stop(task);
        verify(runExecutor, times(0)).stop(task.getCurrentRun());
        task.setCurrentRun(Run.builder().id(1L).build());
        swTaskScheduler.stop(task);
        verify(runExecutor).stop(task.getCurrentRun());
    }

    @Test
    void exec() {
        task.setCurrentRun(Run.builder().id(1L).build());
        swTaskScheduler.exec(task);
        verify(runExecutor).exec(task.getCurrentRun());
    }
}