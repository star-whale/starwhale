/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

import ai.starwhale.mlops.agent.task.EvaluationTask;

import java.io.IOException;
import java.util.List;

/**
 * agent task persistence api
 */
public interface TaskPersistence {

    /**
     * get all tasks
     * @return all tasks
     */
    List<EvaluationTask> getAllActiveTasks() throws IOException;

    /**
     * get task by id
     * @param id key
     * @return task
     */
    EvaluationTask getTaskById(Long id) throws IOException;

    /**
     * save task
     * @param task task
     * @return if success
     */
    boolean save(EvaluationTask task);

    /**
     * move task to the archived state
     * @param task task
     * @return if success
     */
    boolean move2Archived(EvaluationTask task);
}
