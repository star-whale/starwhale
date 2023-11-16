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
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.evaluation.EvaluationUpdateWatcher;
import ai.starwhale.mlops.domain.evaluation.storage.EvaluationRepo;
import ai.starwhale.mlops.domain.ft.FineTuneEvaluationUpdateWatcher;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobDatasetVersionMapper;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.bo.Project;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobDaoTest {

    private JobDao jobDao;

    private EvaluationRepo evaluationRepo;


    @BeforeEach
    public void setUp() {
        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity job1 = JobEntity.builder()
                .id(1L)
                .jobUuid("job-uuid-1")
                .build();
        JobEntity job2 = JobEntity.builder()
                .id(2L)
                .jobUuid("job-uuid-2")
                .build();
        given(jobMapper.findJobById(same(1L)))
                .willReturn(job1);
        given(jobMapper.findJobByUuid(same("job-uuid-2")))
                .willReturn(job2);

        JobDatasetVersionMapper datasetVersionMapper = mock(JobDatasetVersionMapper.class);
        JobBoConverter jobBoConverter = mock(JobBoConverter.class);

        given(jobBoConverter.fromEntity(job1))
                .willReturn(Job.builder().id(1L).uuid("job-uuid-1").build());
        given(jobBoConverter.fromEntity(job2))
                .willReturn(Job.builder().id(2L).uuid("job-uuid-2").build());

        evaluationRepo = mock(EvaluationRepo.class);
        EvaluationUpdateWatcher evalJobWatcher = new EvaluationUpdateWatcher(evaluationRepo);
        FineTuneEvaluationUpdateWatcher ftEvalJobWatcher = new FineTuneEvaluationUpdateWatcher(evaluationRepo);
        jobDao = new JobDao(
                List.of(evalJobWatcher, ftEvalJobWatcher),
                jobMapper,
                datasetVersionMapper,
                new IdConverter(),
                jobBoConverter
        );
    }

    @Test
    public void testUpdateJob() {
        var otherJob = Job.builder().id(1L).uuid("job-uuid-1").build();

        jobDao.updateJobStatus(otherJob, JobStatus.RUNNING);
        verify(evaluationRepo, times(0)).updateJobStatus(any(), any(), any());

        jobDao.updateJobFinishedTime(otherJob, new Date(), 1L);
        verify(evaluationRepo, times(0)).updateJobFinishedTime(any(), any(), any(), any());


        var evalJob = Job.builder()
                .id(1L)
                .uuid("job-uuid-2")
                .project(Project.builder().id(1L).build())
                .type(JobType.EVALUATION)
                .build();
        jobDao.updateJobStatus(evalJob, JobStatus.RUNNING);
        verify(evaluationRepo, times(1))
                .updateJobStatus(eq("project/1/eval/summary"), any(), any());

        jobDao.updateJobFinishedTime(evalJob, new Date(), 1L);
        verify(evaluationRepo, times(1))
                .updateJobFinishedTime(eq("project/1/eval/summary"), any(), any(), any());


        var finetuneEvalJob = Job.builder()
                .id(1L)
                .uuid("job-uuid-3")
                .project(Project.builder().id(1L).build())
                .bizType(BizType.FINE_TUNE)
                .bizId("1")
                .type(JobType.EVALUATION)
                .build();
        jobDao.updateJobStatus(finetuneEvalJob, JobStatus.RUNNING);
        verify(evaluationRepo, times(1))
                .updateJobStatus(eq("project/1/ftspace/1/eval/summary"), any(), any());

        jobDao.updateJobFinishedTime(finetuneEvalJob, new Date(), 1L);
        verify(evaluationRepo, times(1))
                .updateJobFinishedTime(eq("project/1/ftspace/1/eval/summary"), any(), any(), any());
    }

    @Test
    public void testFindJob() {
        var res = jobDao.findJob("1");
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(1L)),
                hasProperty("uuid", is("job-uuid-1"))
        ));

        res = jobDao.findJob("job-uuid-2");
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(2L)),
                hasProperty("uuid", is("job-uuid-2"))
        ));

        res = jobDao.findJob("job-uuid-x");
        assertThat(res, nullValue());
    }
}
