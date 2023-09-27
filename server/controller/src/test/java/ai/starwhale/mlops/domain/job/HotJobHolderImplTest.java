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

import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolderImpl;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link HotJobHolderImpl}
 */
public class HotJobHolderImplTest {

    @Test
    public void testHotJobHolderImpl() {
        final HotJobHolderImpl hotJobHolder = new HotJobHolderImpl(mock(MeterRegistry.class));
        JobMockHolder jobMockHolder = new JobMockHolder();
        Job job1 = jobMockHolder.mockJob();
        Job job2 = jobMockHolder.mockJob();
        job2.getCurrentStep().getTasks().get(0).updateStatus(TaskStatus.FAIL);
        job1.getCurrentStep().getTasks().get(0).updateStatus(TaskStatus.CANCELLING);
        job1.setStatus(JobStatus.CANCELLING);
        hotJobHolder.adopt(job1);
        hotJobHolder.adopt(job2);

        Collection<Job> jobs = hotJobHolder.ofIds(List.of(1L, 8L, 9L));
        Assertions.assertTrue(jobs.contains(job1));
        Assertions.assertTrue(jobs.contains(job2));
        Assertions.assertEquals(2, jobs.size());

        Collection<Job> jobs1 = hotJobHolder.ofStatus(Set.of(JobStatus.CANCELLING));
        Assertions.assertEquals(1, jobs1.size());
        Assertions.assertTrue(jobs1.contains(job1));

        Collection<Task> tasks = List.of(1L, 4L, 5L, 7L, 11L, 12L, 13L, 14L, 15L)
                .stream().map(hotJobHolder::taskWithId)
                .filter(task -> task != null)
                .collect(Collectors.toList());
        Assertions.assertEquals(5, tasks.size());

        hotJobHolder.remove(job1.getId());

        jobs = hotJobHolder.ofIds(List.of(1L, 8L, 9L));
        Assertions.assertTrue(!jobs.contains(job1));
        Assertions.assertTrue(jobs.contains(job2));
        Assertions.assertEquals(1, jobs.size());

        jobs1 = hotJobHolder.ofStatus(Set.of(JobStatus.CANCELLING));
        Assertions.assertEquals(0, jobs1.size());

        tasks = List.of(1L, 4L, 5L, 7L, 11L, 12L, 13L, 14L, 15L)
                .stream().map(hotJobHolder::taskWithId)
                .filter(task -> task != null)
                .collect(Collectors.toList());
        Assertions.assertEquals(3, tasks.size());

    }

}
