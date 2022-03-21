/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.node.Node;

import ai.starwhale.mlops.domain.task.TaskTrigger;
import java.util.List;

/**
 * schedule tasks of jobs
 */
public interface JobScheduler {

    /**
     * scheduler should maintain the jobs and tasks to be scheduled
     * @param job job is better split by the JobScheduler
     */
    void takeJob(Job job);

    /**
     * pop tasks available to the node. if no task is available or the node is full, an empty list should be returned
     * @param node the node load info
     * @return tasks to be assigned to the node
     */
    List<TaskTrigger> schedule(Node node);
}
