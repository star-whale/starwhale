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

package ai.starwhale.mlops.domain.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.job.ExecRequest;
import ai.starwhale.mlops.api.protocol.job.ExecResponse;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.resulting.ResultQuerier;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobServiceForWebTest {

    private JobServiceForWeb service;
    private TaskMapper taskMapper;
    private JobConverter jobConverter;
    private HotJobHolder hotJobHolder;
    private JobLoader jobLoader;
    private ResultQuerier resultQuerier;
    private UserService userService;
    private ProjectService projectService;
    private JobDao jobDao;
    private ModelService modelService;
    private TrashService trashService;
    private SwTaskScheduler swTaskScheduler;

    @BeforeEach
    public void setUp() {
        taskMapper = mock(TaskMapper.class);
        jobConverter = mock(JobConverter.class);
        given(jobConverter.convert(any(JobEntity.class))).willReturn(JobVo.builder().id("1").build());
        hotJobHolder = mock(HotJobHolder.class);
        jobLoader = mock(JobLoader.class);
        resultQuerier = mock(ResultQuerier.class);
        userService = mock(UserService.class);
        projectService = mock(ProjectService.class);
        given(projectService.getProjectId(same("1")))
                .willReturn(1L);
        jobDao = mock(JobDao.class);
        given(jobDao.findJob("1"))
                .willReturn(Job.builder().id(1L).type(JobType.EVALUATION).build());
        given(jobDao.findJobEntity("1"))
                .willReturn(JobEntity.builder().id(1L).type(JobType.EVALUATION).build());
        given(jobDao.getJobId("1"))
                .willReturn(1L);
        given(jobDao.getJobId("2"))
                .willReturn(2L);
        modelService = mock(ModelService.class);
        trashService = mock(TrashService.class);
        swTaskScheduler = mock(SwTaskScheduler.class);

        service = new JobServiceForWeb(
                taskMapper, jobConverter,
                hotJobHolder, projectService, jobDao, jobLoader, resultQuerier, userService,
                mock(JobUpdateHelper.class),
                trashService, swTaskScheduler, mock(JobCreator.class));
    }

    @Test
    public void testListJobs() {
        given(jobDao.listJobs(same(1L), same(1L)))
                .willReturn(List.of(JobEntity.builder().build(), JobEntity.builder().build()));
        var res = service.listJobs("1", 1L, new PageParams(1, 10));
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("pageNum", is(1)),
                hasProperty("size", is(2)),
                hasProperty("list", notNullValue())
        ));
    }

    @Test
    public void testFindJob() {
        var res = service.findJob("", "1");
        assertThat(res, hasProperty("id", is("1")));

        assertThrows(StarwhaleApiException.class,
                () -> service.findJob("", "22"));
    }

    @Test
    public void testGetJobResult() {
        given(resultQuerier.resultOfJob(same(1L)))
                .willReturn("result1");
        var res = service.getJobResult("", "1");
        assertThat(res, is("result1"));
    }

    @Test
    public void testUpdateJobComment() {
        given(jobDao.updateJobComment(same("1"), anyString()))
                .willReturn(true);
        given(jobDao.updateJobComment(same("uuid1"), anyString()))
                .willReturn(true);
        var res = service.updateJobComment("", "1", "comment");
        assertThat(res, is(true));

        res = service.updateJobComment("", "uuid1", "comment");
        assertThat(res, is(true));

        res = service.updateJobComment("", "2", "comment");
        assertThat(res, is(false));
    }

    @Test
    public void testUpdateJobPinStatus() {
        given(jobDao.updateJobPinStatus(same("1"), anyBoolean()))
                .willReturn(true);
        given(jobDao.updateJobPinStatus(same("uuid1"), anyBoolean()))
                .willReturn(true);
        var res = service.updateJobPinStatus("", "1", true);
        assertThat(res, is(true));

        res = service.updateJobPinStatus("", "uuid1", true);
        assertThat(res, is(true));

        res = service.updateJobPinStatus("", "2", true);
        assertThat(res, is(false));
    }

    @Test
    public void testRemoveJob() {
        given(jobDao.removeJob(same(1L))).willReturn(true);
        given(jobDao.removeJobByUuid(same("uuid1"))).willReturn(true);
        given(jobDao.findJob(same("uuid1"))).willReturn(
                Job.builder().id(1L).uuid("uuid1").type(JobType.EVALUATION).build());
        given(jobDao.findJob(same("2"))).willReturn(
                Job.builder().id(2L).type(JobType.EVALUATION).build());

        var res = service.removeJob("", "1");
        assertThat(res, is(true));

        res = service.removeJob("", "uuid1");
        assertThat(res, is(true));

        res = service.removeJob("", "2");
        assertThat(res, is(false));
    }

    @Test
    public void testCancelJob() {
        given(hotJobHolder.ofIds(argThat(argument -> argument.contains(1L))))
                .willReturn(List.of(Job.builder()
                        .steps(List.of(
                                Step.builder()
                                        .tasks(List.of(
                                                Task.builder().id(1L).status(TaskStatus.CANCELED).build(),
                                                Task.builder().id(2L).status(TaskStatus.RUNNING).build(),
                                                Task.builder().id(3L).status(TaskStatus.SUCCESS).build()
                                        )).build()
                        )).status(JobStatus.RUNNING).build()));
        final List<Long> ids = new ArrayList<>();
        doAnswer(invocation -> ids.addAll(invocation.getArgument(0)))
                .when(taskMapper).updateTaskStatus(anyList(), any());
        service.cancelJob("1");
        assertThat(ids, allOf(
                iterableWithSize(2),
                hasItem(1L),
                hasItem(2L)
        ));

        assertThrows(StarwhaleApiException.class,
                () -> service.cancelJob("2"));
    }

    @Test
    public void testPauseJob() {
        given(hotJobHolder.ofIds(argThat(argument -> argument.contains(1L))))
                .willReturn(List.of(Job.builder()
                        .steps(List.of(
                                Step.builder()
                                        .tasks(List.of(
                                                Task.builder().id(1L).status(TaskStatus.CANCELED).build(),
                                                Task.builder().id(2L).status(TaskStatus.RUNNING).build(),
                                                Task.builder().id(3L).status(TaskStatus.SUCCESS).build()
                                        )).build()
                        )).status(JobStatus.RUNNING).build()));
        final List<Long> ids = new ArrayList<>();
        doAnswer(invocation -> ids.addAll(invocation.getArgument(0)))
                .when(taskMapper).updateTaskStatus(anyList(), any());
        service.pauseJob("1");
        assertThat(ids, allOf(
                iterableWithSize(2),
                hasItem(1L),
                hasItem(2L)
        ));

        assertThrows(StarwhaleApiException.class,
                () -> service.cancelJob("22"));
    }

    @Test
    public void testResumeJob() {
        given(jobDao.findJobById(same(1L)))
                .willReturn(Job.builder().status(JobStatus.FAIL).build());
        given(jobDao.findJobById(same(2L)))
                .willReturn(Job.builder().status(JobStatus.SUCCESS).build());
        final List<Job> jobs = new ArrayList<>();
        doAnswer(invocation -> {
            jobs.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(jobLoader).load(any(), any());
        service.resumeJob("1");
        assertThat(jobs, iterableWithSize(1));

        assertThrows(SwValidationException.class,
                () -> service.resumeJob("2"));
    }

    @Test
    public void testAutoReleaseJob() {
        // nothing to do
        when(jobDao.findJobByStatusIn(eq(List.of(JobStatus.RUNNING))))
                .thenReturn(List.of(Job.builder().id(1L).status(JobStatus.RUNNING).build()));

        var svc = spy(service);
        doNothing().when(svc).cancelJob(any());
        svc.gc();
        verify(svc, never()).cancelJob(any());

        // cancel job 2
        var theJob = Job.builder().id(2L).autoReleaseTime(new Date(System.currentTimeMillis() - 1))
                .status(JobStatus.RUNNING).build();
        when(jobDao.findJobByStatusIn(eq(List.of(JobStatus.RUNNING)))).thenReturn(List.of(theJob));
        when(jobDao.getJobId(eq("2"))).thenReturn(theJob.getId());
        when(hotJobHolder.ofIds(eq(List.of(theJob.getId())))).thenReturn(List.of(theJob));

        svc.gc();
        verify(svc).cancelJob("2");
    }

    @Test
    public void testExec() {
        var req = new ExecRequest();
        req.setCommand(new String[]{"ls"});

        assertThrows(SwValidationException.class, () -> service.exec("1", "2", "3", req));

        var task = Task.builder().id(3L).status(TaskStatus.RUNNING).build();
        var step = Step.builder().id(3L).tasks(List.of(task)).build();
        var job = Job.builder().id(2L).status(JobStatus.RUNNING).steps(List.of(step)).build();

        when(jobDao.findJobById(eq(job.getId()))).thenReturn(job);
        when(hotJobHolder.ofIds(eq(List.of(job.getId())))).thenReturn(List.of(job));
        when(hotJobHolder.ofIds(eq(List.of(step.getId())))).thenReturn(List.of(job));
        when(hotJobHolder.ofIds(eq(List.of(task.getId())))).thenReturn(List.of(job));

        var expected = ExecResponse.builder().stdout("stdout").stderr("stderr").build();
        when(swTaskScheduler.exec(eq(task), any())).thenReturn(new Future<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String[] get() {
                return new String[]{"stdout", "stderr"};
            }

            @Override
            public String[] get(long timeout, @NotNull TimeUnit unit) {
                return get();
            }
        });
        var resp = service.exec("1", "2", "3", req);
        assertEquals(expected, resp);
    }
}
