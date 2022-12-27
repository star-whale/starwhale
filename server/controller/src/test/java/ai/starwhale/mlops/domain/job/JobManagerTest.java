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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.storage.JobRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobManagerTest {

    private JobManager manager;

    @BeforeEach
    public void setUp() {
        JobRepo jobRepo = mock(JobRepo.class);
        JobEntity job1 = JobEntity.builder()
                .id(1L)
                .jobUuid("job-uuid-1")
                .build();
        JobEntity job2 = JobEntity.builder()
                .id(2L)
                .jobUuid("job-uuid-2")
                .build();
        given(jobRepo.findJobById(same(1L)))
                .willReturn(job1);
        given(jobRepo.findJobByUuid(same("job-uuid-2")))
                .willReturn(job2);
        manager = new JobManager(jobRepo, new IdConverter());
    }

    @Test
    public void testFindJob() {
        var res = manager.findJob("1");
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(1L)),
                hasProperty("jobUuid", is("job-uuid-1"))
        ));

        res = manager.findJob("job-uuid-2");
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(2L)),
                hasProperty("jobUuid", is("job-uuid-2"))
        ));

        res = manager.findJob("job-uuid-x");
        assertThat(res, nullValue());
    }
}
