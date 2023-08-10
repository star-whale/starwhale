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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.api.protobuf.Model.Env;
import ai.starwhale.mlops.api.protobuf.Model.StepSpec;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.split.JobSpliteratorImpl;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.exception.SwValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * test for {@link JobSpliteratorImpl}
 */
public class JobSpliteratorImplTest {

    @Test
    public void testJobSpliteratorEvaluation() throws JsonProcessingException {
        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockJob = jobMockHolder.mockJob();
        mockJob.setCurrentStep(null);
        mockJob.setSteps(null);
        mockJob.setStatus(JobStatus.CREATED);
        var steps = List.of(
                StepSpec.newBuilder()
                    .setJobName("evaluate")
                    .setName("predict")
                    .setReplicas(1)
                    .addAllResources(List.of())
                    .addAllEnv(List.of(Env.newBuilder().setName("SW_TEST").setValue("val-a").build()))
                    .build(),
                StepSpec.newBuilder()
                    .setJobName("evaluate")
                    .setName("evaluate")
                    .setReplicas(1)
                    .addAllResources(List.of())
                    .addAllEnv(List.of(Env.newBuilder().setName("SW_TEST").setValue("val-b").build()))
                    .addAllNeeds(List.of("predict"))
                    .build()
        );
        TaskMapper taskMapper = mock(TaskMapper.class);
        JobDao jobDao = mock(JobDao.class);
        StepMapper stepMapper = mock(StepMapper.class);
        JobSpecParser jobParser = mock(JobSpecParser.class);
        given(jobParser.parseAndFlattenStepFromYaml(any())).willReturn(steps);

        JobSpliteratorImpl jobSpliteratorImpl = new JobSpliteratorImpl(
                new StoragePathCoordinator("/test"), taskMapper, jobDao, stepMapper, jobParser);

        mockJob.setStepSpec("");
        Assertions.assertThrows(SwValidationException.class, () -> jobSpliteratorImpl.split(mockJob));

        mockJob.setStepSpec("123");
        mockJob.setResourcePool(ResourcePool.builder().name("test").build());
        var stepEntities = jobSpliteratorImpl.split(mockJob);
        assertEquals(stepEntities.size(), 2);
        verify(stepMapper, times(2)).save(any());
        verify(stepMapper, times(2)).updateLastStep(any(), any());
        verify(taskMapper, times(2)).addAll(any());
        verify(jobDao, times(1)).updateJobStatus(any(), any());

        mockJob.setStatus(JobStatus.RUNNING);
        Assertions.assertThrows(SwValidationException.class, () -> jobSpliteratorImpl.split(mockJob));

    }
}
