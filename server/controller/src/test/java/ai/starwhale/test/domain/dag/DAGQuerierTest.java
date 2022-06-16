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

package ai.starwhale.test.domain.dag;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dag.DAGQuerier;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.test.JobMockHolder;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link ai.starwhale.mlops.domain.dag.DAGQuerier}
 */
public class DAGQuerierTest {


    @Test
    public void testDAGQuerier() {
        JobManager jobManager = mock(JobManager.class);
        when(jobManager.getJobId("1")).thenReturn(1L);
        when(jobManager.getJobId("2")).thenReturn(2L);
        HotJobHolder hotJobHolder = mock(HotJobHolder.class);
        when(hotJobHolder.ofIds(List.of(1L))).thenReturn(Collections.emptySet());
        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockedJob = jobMockHolder.mockJob();
        when(hotJobHolder.ofIds(List.of(2L))).thenReturn(List.of(mockedJob));

        JobMapper jobMapper = mock(JobMapper.class);
        JobEntity jobEntity = JobEntity.builder().id(1L).jobStatus(JobStatus.RUNNING).build();
        when(jobMapper.findJobById(1L)).thenReturn(jobEntity);

        JobLoader jobLoader = mock(JobLoader.class);
        when(jobLoader.loadEntities(List.of(jobEntity),false,false)).thenReturn(List.of(mockedJob));

        DAGQuerier dagQuerier = new DAGQuerier(jobManager,hotJobHolder,jobMapper,new StepHelper(), jobLoader);
        Graph graph = dagQuerier.dagOfJob("1",true);
        Assertions.assertEquals(3,graph.getGroupingNodes().keySet().size());


        Graph graph2 = dagQuerier.dagOfJob("2",true);
        Assertions.assertEquals(3,graph2.getGroupingNodes().keySet().size());


    }


}
