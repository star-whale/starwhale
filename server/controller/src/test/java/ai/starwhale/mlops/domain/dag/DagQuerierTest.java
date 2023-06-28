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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepService;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.task.TaskService;
import ai.starwhale.mlops.exception.SwValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link DagQuerier}
 */
public class DagQuerierTest {


    @Test
    public void testDagQuerier() {
        Job mockedJob = new JobMockHolder().mockJob();
        JobDao jobDao = mock(JobDao.class);
        when(jobDao.findJob(any())).thenReturn(mockedJob);
        DagQuerier dagQuerier = new DagQuerier(jobDao,
                new StepService(mock(StepMapper.class), mock(StepConverter.class), mock(TaskService.class)));
        Graph graph = dagQuerier.dagOfJob("1");
        Assertions.assertEquals(3, graph.getGroupingNodes().keySet().size());
        Assertions.assertEquals(4, graph.getGroupingNodes().get("Task").size());
        Assertions.assertEquals(2, graph.getGroupingNodes().get("Step").size());
        Assertions.assertEquals(2, graph.getGroupingNodes().get("Job").size());
        Assertions.assertEquals(9, graph.getEdges().size());

        mockedJob.setStatus(JobStatus.CREATED);
        Assertions.assertThrowsExactly(SwValidationException.class, () -> dagQuerier.dagOfJob("3"));


    }


}
