/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Node;

import ai.starwhale.mlops.domain.task.TaskTrigger;
import java.util.List;
import java.util.Collection;

/**
 * schedule tasks of jobs
 */
public interface TaskScheduler {


    /**
     * scheduler should maintain the tasks to be scheduled
     * @param TaskTriggers tasks to be scheduled
     * @param deviceClass the device type should be scheduled on
     */
    void adoptTasks(Collection<TaskTrigger> TaskTriggers, Device.Clazz deviceClass);

    /**
     * pop tasks available to the node. if no task is available or the node is full, an empty list should be returned
     * TaskStatus -> Assigning
     * @param node the node load info
     * @return tasks to be assigned to the node
     */
    List<TaskTrigger> schedule(Node node);
}
