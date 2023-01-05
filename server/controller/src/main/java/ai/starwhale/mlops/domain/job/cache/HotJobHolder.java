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
import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.Collection;
import java.util.Set;

/**
 * holds all the running jobs
 */
public interface HotJobHolder {

    void adopt(Job job);

    Collection<Job> ofIds(Collection<String> ids);

    Collection<Job> ofStatus(Set<JobStatus> jobStatuses);

    Collection<Task> tasksOfIds(Collection<Long> taskIds);

    /**
     * remove job in cache
     *
     */
    void remove(String jobId);
}
