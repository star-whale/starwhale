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

package ai.starwhale.test.domain.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.trigger.StepTriggerContext;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.po.AgentEntity;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.WatchableTask;
import ai.starwhale.mlops.domain.task.status.WatchableTaskFactory;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.schedule.CommandingTasksAssurance;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import ai.starwhale.test.JobMockHolder;
import ai.starwhale.test.ObjectMockHolder;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link JobLoader}
 */
public class JobLoaderTest {


    @Test
    public void testJobLoaderCacheOnly() {

        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity jobEntity = JobEntity.builder().id(1L).jobStatus(JobStatus.RUNNING).build();
        when(jobMapper.findJobById(1L)).thenReturn(
            jobEntity);
        StepMapper stepMapper = mock(StepMapper.class);
        when(stepMapper.findByJobId(1L)).thenReturn(List.of(
                StepEntity.builder().id(1L).status(StepStatus.RUNNING).name("PPL").build()
                , StepEntity.builder().id(2L).lastStepId(1L).status(StepStatus.CREATED).name("CMP")
                    .build()
            )
        );
        TaskMapper taskMapper = mock(TaskMapper.class);

        when(taskMapper.findByStepId(1L)).thenReturn(List.of(
            TaskEntity.builder().id(1L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.RUNNING)
                .build()
            , TaskEntity.builder().id(2L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.RUNNING)
                .build()
        ));

        when(taskMapper.findByStepId(2L)).thenReturn(List.of(
            TaskEntity.builder().id(3L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.CMP).taskRequest("a\nb\nc").taskStatus(TaskStatus.CREATED)
                .build()
        ));

        Job mockJob = new JobMockHolder().mockJob();
        mockJob.setSteps(null);
        mockJob.setCurrentStep(null);
        SWTaskScheduler swTaskScheduler = mock(SWTaskScheduler.class);
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = ObjectMockHolder.taskBoConverter();
        JobBoConverter jobBoConverter = mock(JobBoConverter.class);
        when(jobBoConverter.fromEntity(any(JobEntity.class))).thenReturn(mockJob);
        HotJobHolder jobHolder = mock(HotJobHolder.class);
        StepConverter stepConverter = new StepConverter(new LocalDateTimeConvertor());
        WatchableTaskFactory watchableTaskFactory = mock(WatchableTaskFactory.class);
        StepTriggerContext stepTriggerContext = mock(StepTriggerContext.class);
        JobUpdateHelper jobUpdateHelper = mock(JobUpdateHelper.class);

        JobLoader jobLoader = new JobLoader(swTaskScheduler, commandingTasksAssurance, taskMapper,
            taskBoConverter, jobBoConverter, jobHolder, stepMapper, stepConverter,
            watchableTaskFactory, stepTriggerContext, new StepHelper(), jobUpdateHelper);

        List<Job> jobs = jobLoader.loadEntities(List.of(jobEntity), false, true);
        Assertions.assertEquals(1, jobs.size());
        Job loadedJob = jobs.get(0);
        Assertions.assertEquals(2, loadedJob.getSteps().size());
        Assertions.assertNotNull(loadedJob.getCurrentStep());
        Assertions.assertNotNull(loadedJob.getCurrentStep().getNextStep());

        verify(jobHolder, times(1)).adopt(mockJob);
        verify(swTaskScheduler, times(0)).adoptTasks(anyCollection(), any(Clazz.class));
        verify(commandingTasksAssurance, times(0)).onTaskCommanding(anyList(), any(Agent.class));
        verify(watchableTaskFactory, times(2)).wrapTasks(anyCollection());
        loadedJob.getSteps().parallelStream().map(Step::getTasks).flatMap(Collection::stream)
            .forEach(t -> {
                Assertions.assertTrue(t instanceof WatchableTask);
            });

    }

    @Test
    public void testJobLoaderResume() {

        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity jobEntity = JobEntity.builder().id(1L).jobStatus(JobStatus.RUNNING).build();
        when(jobMapper.findJobById(1L)).thenReturn(
            jobEntity);
        StepMapper stepMapper = mock(StepMapper.class);
        when(stepMapper.findByJobId(1L)).thenReturn(List.of(
                StepEntity.builder().id(1L).status(StepStatus.FAIL).name("PPL").build()
                , StepEntity.builder().id(2L).lastStepId(1L).status(StepStatus.CREATED).name("CMP")
                    .build()
            )
        );
        TaskMapper taskMapper = mock(TaskMapper.class);

        when(taskMapper.findByStepId(1L)).thenReturn(List.of(
            TaskEntity.builder().id(1L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.ASSIGNING)
                .agent(
                    AgentEntity.builder().id(1L).serialNumber("serial")
                        .connectTime(LocalDateTime.now()).build()).build()
            , TaskEntity.builder().id(2L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.FAIL)
                .build()
        ));

        when(taskMapper.findByStepId(2L)).thenReturn(List.of(
            TaskEntity.builder().id(3L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.CMP).taskRequest("a\nb\nc").taskStatus(TaskStatus.CREATED)
                .build()
        ));

        Job mockJob = new JobMockHolder().mockJob();
        mockJob.setSteps(null);
        mockJob.setCurrentStep(null);
        mockJob.setStatus(JobStatus.FAIL);
        SWTaskScheduler swTaskScheduler = mock(SWTaskScheduler.class);
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = ObjectMockHolder.taskBoConverter();
        JobBoConverter jobBoConverter = mock(JobBoConverter.class);
        when(jobBoConverter.fromEntity(any(JobEntity.class))).thenReturn(mockJob);
        HotJobHolder jobHolder = mock(HotJobHolder.class);
        StepConverter stepConverter = new StepConverter(new LocalDateTimeConvertor());
        WatchableTaskFactory watchableTaskFactory = new WatchableTaskFactory(List.of(),
            new TaskStatusMachine());
        StepTriggerContext stepTriggerContext = mock(StepTriggerContext.class);
        JobUpdateHelper jobUpdateHelper = mock(JobUpdateHelper.class);

        JobLoader jobLoader = new JobLoader(swTaskScheduler, commandingTasksAssurance, taskMapper,
            taskBoConverter, jobBoConverter, jobHolder, stepMapper, stepConverter,
            watchableTaskFactory, stepTriggerContext, new StepHelper(), jobUpdateHelper);

        List<Job> jobs = jobLoader.loadEntities(List.of(jobEntity), true, true);
        Assertions.assertEquals(1, jobs.size());
        Job loadedJob = jobs.get(0);
        Assertions.assertEquals(2, loadedJob.getSteps().size());
        Assertions.assertNotNull(loadedJob.getCurrentStep());
        Assertions.assertEquals(StepStatus.RUNNING, loadedJob.getCurrentStep().getStatus());
        Assertions.assertNotNull(loadedJob.getCurrentStep().getNextStep());

        verify(jobUpdateHelper).updateJob(mockJob);
        verify(jobHolder, times(1)).adopt(mockJob);
        verify(swTaskScheduler, times(1)).adoptTasks(anyCollection(), any(Clazz.class));
        verify(commandingTasksAssurance, times(1)).onTaskCommanding(anyList(), any(Agent.class));
        Set<Task> tasks = loadedJob.getSteps().parallelStream().map(Step::getTasks)
            .flatMap(Collection::stream).collect(
                Collectors.toSet());
        Assertions.assertEquals(3, tasks.size());
        tasks.forEach(t -> {
            Assertions.assertTrue(t instanceof WatchableTask);
        });


    }


    @Test
    public void testJobLoaderNoCache() {

        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity jobEntity = JobEntity.builder().id(1L).jobStatus(JobStatus.RUNNING).build();
        when(jobMapper.findJobById(1L)).thenReturn(
            jobEntity);
        StepMapper stepMapper = mock(StepMapper.class);
        when(stepMapper.findByJobId(1L)).thenReturn(List.of(
                StepEntity.builder().id(1L).status(StepStatus.FAIL).name("PPL").build()
                , StepEntity.builder().id(2L).lastStepId(1L).status(StepStatus.CREATED).name("CMP")
                    .build()
            )
        );
        TaskMapper taskMapper = mock(TaskMapper.class);

        when(taskMapper.findByStepId(1L)).thenReturn(List.of(
            TaskEntity.builder().id(1L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.ASSIGNING)
                .agent(
                    AgentEntity.builder().id(1L).serialNumber("serial")
                        .connectTime(LocalDateTime.now()).build()).build()
            , TaskEntity.builder().id(2L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.FAIL)
                .build()
        ));

        when(taskMapper.findByStepId(2L)).thenReturn(List.of(
            TaskEntity.builder().id(3L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.CMP).taskRequest("a\nb\nc").taskStatus(TaskStatus.CREATED)
                .build()
        ));

        Job mockJob = new JobMockHolder().mockJob();
        mockJob.setSteps(null);
        mockJob.setCurrentStep(null);
        mockJob.setStatus(JobStatus.FAIL);
        SWTaskScheduler swTaskScheduler = mock(SWTaskScheduler.class);
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = ObjectMockHolder.taskBoConverter();
        JobBoConverter jobBoConverter = mock(JobBoConverter.class);
        when(jobBoConverter.fromEntity(any(JobEntity.class))).thenReturn(mockJob);
        HotJobHolder jobHolder = mock(HotJobHolder.class);
        StepConverter stepConverter = new StepConverter(new LocalDateTimeConvertor());
        WatchableTaskFactory watchableTaskFactory = new WatchableTaskFactory(List.of(),
            new TaskStatusMachine());
        StepTriggerContext stepTriggerContext = mock(StepTriggerContext.class);
        JobUpdateHelper jobUpdateHelper = mock(JobUpdateHelper.class);

        JobLoader jobLoader = new JobLoader(swTaskScheduler, commandingTasksAssurance, taskMapper,
            taskBoConverter, jobBoConverter, jobHolder, stepMapper, stepConverter,
            watchableTaskFactory, stepTriggerContext, new StepHelper(), jobUpdateHelper);

        List<Job> jobs = jobLoader.loadEntities(List.of(jobEntity), false, false);
        Assertions.assertEquals(1, jobs.size());
        Job loadedJob = jobs.get(0);
        Assertions.assertEquals(2, loadedJob.getSteps().size());
        Assertions.assertNull(loadedJob.getCurrentStep());

        verify(jobUpdateHelper).updateJob(mockJob);
        verify(jobHolder, times(0)).adopt(mockJob);
        verify(swTaskScheduler, times(0)).adoptTasks(anyCollection(), any(Clazz.class));
        verify(commandingTasksAssurance, times(0)).onTaskCommanding(anyList(), any(Agent.class));
        Set<Task> tasks = loadedJob.getSteps().parallelStream().map(Step::getTasks)
            .flatMap(Collection::stream).collect(
                Collectors.toSet());
        Assertions.assertEquals(3, tasks.size());
        tasks.forEach(t -> {
            Assertions.assertTrue(!(t instanceof WatchableTask));
        });

//        jobs = jobLoader.loadEntities(List.of(jobEntity), false, true);
//
//        jobs = jobLoader.loadEntities(List.of(jobEntity), true, true);
//

    }

    @Test
    public void testException() {
        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity jobEntity = JobEntity.builder().id(1L).jobStatus(JobStatus.RUNNING).build();
        when(jobMapper.findJobById(1L)).thenReturn(
            jobEntity);
        StepMapper stepMapper = mock(StepMapper.class);
        when(stepMapper.findByJobId(1L)).thenReturn(List.of(
                StepEntity.builder().id(1L).status(StepStatus.FAIL).name("PPL").build()
                , StepEntity.builder().id(2L).lastStepId(1L).status(StepStatus.CREATED).name("CMP")
                    .build()
            )
        );
        TaskMapper taskMapper = mock(TaskMapper.class);

        when(taskMapper.findByStepId(1L)).thenReturn(List.of(
            TaskEntity.builder().id(1L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.ASSIGNING)
                .agent(
                    AgentEntity.builder().id(1L).serialNumber("serial")
                        .connectTime(LocalDateTime.now()).build()).build()
            , TaskEntity.builder().id(2L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.PPL).taskRequest(TASK_REQUEST).taskStatus(TaskStatus.FAIL)
                .build()
        ));

        when(taskMapper.findByStepId(2L)).thenReturn(List.of(
            TaskEntity.builder().id(3L).taskUuid(UUID.randomUUID().toString())
                .taskType(TaskType.CMP).taskRequest("a\nb\nc").taskStatus(TaskStatus.CREATED)
                .build()
        ));

        Job mockJob = new JobMockHolder().mockJob();
        mockJob.setSteps(null);
        mockJob.setCurrentStep(null);
        mockJob.setStatus(JobStatus.FAIL);
        SWTaskScheduler swTaskScheduler = mock(SWTaskScheduler.class);
        CommandingTasksAssurance commandingTasksAssurance = mock(CommandingTasksAssurance.class);
        TaskBoConverter taskBoConverter = ObjectMockHolder.taskBoConverter();
        JobBoConverter jobBoConverter = mock(JobBoConverter.class);
        when(jobBoConverter.fromEntity(any(JobEntity.class))).thenReturn(mockJob);
        HotJobHolder jobHolder = mock(HotJobHolder.class);
        StepConverter stepConverter = new StepConverter(new LocalDateTimeConvertor());
        WatchableTaskFactory watchableTaskFactory = new WatchableTaskFactory(List.of(),
            new TaskStatusMachine());
        StepTriggerContext stepTriggerContext = mock(StepTriggerContext.class);
        JobUpdateHelper jobUpdateHelper = mock(JobUpdateHelper.class);

        JobLoader jobLoader = new JobLoader(swTaskScheduler, commandingTasksAssurance, taskMapper,
            taskBoConverter, jobBoConverter, jobHolder, stepMapper, stepConverter,
            watchableTaskFactory, stepTriggerContext, new StepHelper(), jobUpdateHelper);

        Assertions.assertThrowsExactly(SWProcessException.class, () -> {
            jobLoader.loadEntities(List.of(jobEntity), true, false);
        });

    }

    final static String TASK_REQUEST = "{\"id\":99,\"dsName\":\"cifar10\",\"dsVersion\":\"g4ytezbqgfrtmodche3wcnrupfwta5a\",\"batch\":50,\"label\":{\"offset\":4064,\"size\":4064,\"file\":\"StarWhale/controller/swds/cifar10/g4ytezbqgfrtmodche3wcnrupfwta5a/label_ubyte_7.swds_bin\"},\"data\":{\"offset\":155616,\"size\":155616,\"file\":\"StarWhale/controller/swds/cifar10/g4ytezbqgfrtmodche3wcnrupfwta5a/data_ubyte_7.swds_bin\"}}";
}
