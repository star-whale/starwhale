/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Node;

import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.List;
import java.util.Collection;

/**
 * schedule tasks of jobs
 */
public interface SWTaskScheduler {


    /**
     * scheduler should maintain the tasks to be scheduled
     * @param tasks tasks to be scheduled
     * @param deviceClass the device type should be scheduled on
     */
    void adoptTasks(Collection<Task> tasks, Device.Clazz deviceClass);

    /**
     * @param taskIds tasks to be stop scheduled
     */
    void stopSchedule(Collection<Long> taskIds);

    /**
     * pop tasks available to the node. if no task is available or the node is full, an empty list should be returned
     * TaskStatus -> Created.DOING
     * @param node the node load info
     * @return tasks to be assigned to the node
     */
    List<Task> schedule(Node node);
}
