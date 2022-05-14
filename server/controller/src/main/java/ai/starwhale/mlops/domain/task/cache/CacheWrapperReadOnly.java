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

package ai.starwhale.mlops.domain.task.cache;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.Task.StatusUnModifiableTask;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CacheWrapperReadOnly implements LivingTaskCache{

    final LivingTaskCache livingTaskCache;

    public CacheWrapperReadOnly(LivingTaskCache livingTaskCache) {
        this.livingTaskCache = livingTaskCache;
    }


    @Override
    public void adopt(Collection<Task> livingTasks, TaskStatus status) {
        livingTaskCache.adopt(livingTasks,status);
    }

    @Override
    public void update(Long taskId, TaskStatus newStatus) {
        livingTaskCache.update(taskId,newStatus);
    }

    @Override
    public Collection<Task> ofStatus(TaskStatus taskStatus) {
        return wrapTasks(livingTaskCache.ofStatus(taskStatus));
    }

    private List<Task> wrapTasks(Collection<Task> tasks) {
        return tasks.parallelStream().map(t -> new StatusUnModifiableTask(t))
            .collect(
                Collectors.toList());
    }

    @Override
    public Collection<Task> ofIds(Collection<Long> taskIds) {
        return wrapTasks(livingTaskCache.ofIds(taskIds));
    }

    @Override
    public Collection<Task> ofJob(Long jobId) {
        return wrapTasks(livingTaskCache.ofJob(jobId));
    }

    @Override
    public void clearTasksOf(Long jobId) {
        livingTaskCache.clearTasksOf(jobId);
    }
}
