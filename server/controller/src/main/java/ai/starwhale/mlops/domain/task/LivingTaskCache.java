/**
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

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Collection;
import java.util.Optional;

/**
 * manage status of Tasks or side effects caused by change of Task status (Job status change)
 */
public interface LivingTaskCache {

    /**
     * simply adds to the cache
     * @param livingTasks
     * @param status
     */
    void adopt(Collection<Task> livingTasks, TaskStatus status);

    /**
     * do business logic caused by status change
     * @param livingTaskIds
     * @param status
     */
    void update(Collection<Long> livingTaskIds, TaskStatus status);

    /**
     *
     * @param taskStatus
     * @return better if the client can't modify the task returned
     */
    Collection<Task> ofStatus(TaskStatus taskStatus);

    /**
     *
     * @param taskId
     * @return better if the client can't modify the task returned
     */
    Optional<Task> ofId(Long taskId);

    /**
     *
     * @param jobId
     * @return better if the client can't modify the task returned
     */
    Collection<Task> ofJob(Long jobId);

}
