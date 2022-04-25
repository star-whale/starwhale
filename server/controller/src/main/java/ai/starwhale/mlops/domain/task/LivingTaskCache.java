/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
     * @param livingTasks
     * @param status
     */
    void update(Collection<Task> livingTasks, TaskStatus status);

    /**
     *
     * @param taskStatus
     * @return deep copy of the tasks cached
     */
    Collection<Task> ofStatus(TaskStatus taskStatus);

    /**
     *
     * @param taskId
     * @return deep copy of the tasks cached
     */
    Optional<Task> ofId(Long taskId);

    /**
     *
     * @param jobId
     * @return deep copy of the tasks cached
     */
    Collection<Task> ofJob(Long jobId);

}
