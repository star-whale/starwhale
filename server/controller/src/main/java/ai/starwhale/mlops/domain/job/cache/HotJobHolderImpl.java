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

package ai.starwhale.mlops.domain.job.cache;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * holds all the running jobs
 */
@Service
public class HotJobHolderImpl implements HotJobHolder {

    ConcurrentHashMap<Long, Job> jobMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<Long, Task> taskMap = new ConcurrentHashMap<>();

    public HotJobHolderImpl(MeterRegistry meterRegistry) {
        for (JobStatus jobStatus : JobStatus.values()) {
            meterRegistry.gauge("sw.job.cache.size", Tags.of("status", jobStatus.name()), jobMap,
                    (jobMap) -> jobMap.values().stream().filter(job -> job.getStatus() == jobStatus).count());
        }
        for (TaskStatus taskStatus : TaskStatus.values()) {
            meterRegistry.gauge("sw.task.cache.size", Tags.of("status", taskStatus.name()), taskMap,
                    (taskMap) -> taskMap.values().stream().filter(task -> task.getStatus() == taskStatus).count());
        }
    }

    public void adopt(Job job) {
        jobMap.put(job.getId(), job);
        job.getSteps().stream().map(Step::getTasks)
                .flatMap(Collection::stream)
                .forEach(task -> taskMap.put(task.getId(), task));
    }

    public Collection<Job> ofIds(Collection<Long> ids) {
        return ids.parallelStream().map(id -> jobMap.get(id)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public Collection<Job> ofStatus(Set<JobStatus> jobStatuses) {
        return jobMap.values().stream()
                .filter(job -> jobStatuses.contains(job.getStatus()))
                .collect(Collectors.toList());
    }

    public Task taskWithId(Long taskId) {
        return taskMap.get(taskId);
    }

    /**
     * remove job in cache
     */
    public void remove(Long jobId) {
        Job job = jobMap.get(jobId);
        if (null == job) {
            return;
        }
        job.getSteps().stream().map(Step::getTasks)
                .flatMap(Collection::stream)
                .map(Task::getId)
                .forEach(tid -> taskMap.remove(tid));
        jobMap.remove(jobId);
    }
}
