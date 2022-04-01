/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.Collection;
import java.util.Optional;

/**
 * manage status of Tasks or side effects caused by change of Task status (Job status change)
 */
public interface LivingTaskStatusMachine {

    void adopt(Collection<Task> livingTasks, TaskStatus status);

    Collection<Task> ofStatus(TaskStatus taskStatus);

    Optional<Task> ofId(Long taskId);

}
