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

package ai.starwhale.mlops.domain.dag;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link DagQuerier}
 */
public class DagQuerierTest {


    @Test
    public void testDagQuerier() {
        JobManager jobManager = mock(JobManager.class);
        when(jobManager.getJobId("1")).thenReturn(1L);
        when(jobManager.getJobId("2")).thenReturn(2L);
        when(jobManager.getJobId("3")).thenReturn(3L);
        HotJobHolder hotJobHolder = mock(HotJobHolder.class);
        when(hotJobHolder.ofIds(List.of(1L))).thenReturn(Collections.emptySet());
        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockedJob = jobMockHolder.mockJob();
        Job createdJob = jobMockHolder.mockJob();
        createdJob.setStatus(JobStatus.CREATED);
        when(hotJobHolder.ofIds(List.of(2L))).thenReturn(List.of(mockedJob));
        when(hotJobHolder.ofIds(List.of(3L))).thenReturn(List.of(createdJob));

        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity jobEntity = JobEntity.builder().id(1L).jobStatus(JobStatus.RUNNING).build();
        when(jobMapper.findJobById(1L)).thenReturn(jobEntity);

        JobLoader jobLoader = mock(JobLoader.class);
        when(jobLoader.loadEntities(List.of(jobEntity), false, false)).thenReturn(List.of(mockedJob));

        DagQuerier dagQuerier = new DagQuerier(jobManager, hotJobHolder, jobMapper, new StepHelper(), jobLoader);
        Graph graph = dagQuerier.dagOfJob("1", true);
        Assertions.assertEquals(3, graph.getGroupingNodes().keySet().size());

        Graph graph2 = dagQuerier.dagOfJob("2", true);
        Assertions.assertEquals(3, graph2.getGroupingNodes().keySet().size());

        Assertions.assertThrowsExactly(SwValidationException.class, () -> dagQuerier.dagOfJob("3", true));


    }


}
