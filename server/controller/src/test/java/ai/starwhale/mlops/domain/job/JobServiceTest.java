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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.doAnswer;

import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.resulting.ResultQuerier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobServiceTest {

    private JobService service;
    private TaskMapper taskMapper;
    private JobConverter jobConverter;
    private JobBoConverter jobBoConverter;
    private JobSpliterator jobSpliterator;
    private HotJobHolder hotJobHolder;
    private JobLoader jobLoader;
    private ResultQuerier resultQuerier;
    private StoragePathCoordinator storagePathCoordinator;
    private UserService userService;
    private ProjectManager projectManager;
    private JobDao jobDao;
    private ModelDao modelDao;
    private DatasetDao datasetDao;
    private RuntimeDao runtimeDao;
    private TrashService trashService;

    @BeforeEach
    public void setUp() {
        taskMapper = mock(TaskMapper.class);
        jobConverter = mock(JobConverter.class);
        jobBoConverter = mock(JobBoConverter.class);
        given(jobConverter.convert(any(Job.class))).willReturn(JobVo.builder().id("1").build());
        jobSpliterator = mock(JobSpliterator.class);
        hotJobHolder = mock(HotJobHolder.class);
        jobLoader = mock(JobLoader.class);
        resultQuerier = mock(ResultQuerier.class);
        storagePathCoordinator = mock(StoragePathCoordinator.class);
        userService = mock(UserService.class);
        projectManager = mock(ProjectManager.class);
        given(projectManager.getProjectId(same("1")))
                .willReturn(1L);
        jobDao = mock(JobDao.class);
        given(jobDao.findJob("1"))
                .willReturn(Job.builder().id(1L).build());
        given(jobDao.getJobId("1"))
                .willReturn(1L);
        given(jobDao.getJobId("2"))
                .willReturn(2L);
        modelDao = mock(ModelDao.class);
        datasetDao = mock(DatasetDao.class);
        runtimeDao = mock(RuntimeDao.class);
        trashService = mock(TrashService.class);

        service = new JobService(
                taskMapper, jobConverter, jobBoConverter, runtimeDao, jobSpliterator,
                hotJobHolder, projectManager, jobDao, jobLoader, modelDao,
                resultQuerier, datasetDao, storagePathCoordinator, userService, mock(JobUpdateHelper.class),
                trashService);
    }

    @Test
    public void testListJobs() {
        given(jobDao.listJobs(same(1L), same(1L)))
                .willReturn(List.of(Job.builder().build(), Job.builder().build()));
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
    public void testRemoveJob() {
        given(jobDao.removeJob(same(1L))).willReturn(true);
        given(jobDao.removeJobByUuid(same("uuid1"))).willReturn(true);
        given(jobDao.getJobId(same("uuid1"))).willReturn(1L);

        var res = service.removeJob("", "1");
        assertThat(res, is(true));

        res = service.removeJob("", "uuid1");
        assertThat(res, is(true));

        res = service.removeJob("", "2");
        assertThat(res, is(false));
    }

    @Test
    public void testCreateJob() {
        given(userService.currentUserDetail())
                .willReturn(User.builder().id(1L).build());
        given(projectManager.getProject(anyString()))
                .willReturn(ProjectEntity.builder().id(1L).projectName("test-project").build());
        given(runtimeDao.getRuntimeVersion(same("2")))
                .willReturn(RuntimeVersionEntity.builder().id(2L).runtimeId(2L).versionName("1r2t3y4u5i6").build());
        given(runtimeDao.getRuntime(same(2L)))
                .willReturn(RuntimeEntity.builder().id(2L).runtimeName("test-runtime").build());
        given(modelDao.getModelVersion(same("3")))
                .willReturn(ModelVersionEntity.builder().id(3L).modelId(3L).versionName("q1w2e3r4t5y6").build());
        given(modelDao.getModel(same(3L)))
                .willReturn(ModelEntity.builder().id(3L).modelName("test-model").build());
        given(storagePathCoordinator.allocateResultMetricsPath("uuid1"))
                .willReturn("out");
        given(jobDao.addJob(any(JobFlattenEntity.class)))
                .willAnswer(invocation -> {
                    JobFlattenEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return true;
                });
        given(datasetDao.getDatasetVersion(anyString()))
                .willReturn(DatasetVersion.builder().id(1L).versionName("a1s2d3f4g5h6").build());

        var res = service.createJob("1", "3", "1", "2",
                 "", "1", "stepSpec1");
        assertThat(res, is(1L));

        res = service.createJob("1", "3", "1", "2",
                "", "1", "stepSpec2");
        assertThat(res, is(1L));
    }

    @Test
    public void testSplitNewCreatedJobs() {
        given(jobDao.findJobByStatusIn(any()))
                .willReturn(List.of(Job.builder().id(1L).build()));
        given(jobDao.findJobById(same(1L)))
                .willReturn(Job.builder().id(1L).build());
        final List<Job> jobs = new ArrayList<>();
        doAnswer(invocation -> {
            jobs.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(jobLoader).load(any(), anyBoolean());

        service.splitNewCreatedJobs();
        assertThat(jobs, allOf(
                is(iterableWithSize(1)),
                is(hasItem(hasProperty("id", is(1L))))
        ));
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

}
