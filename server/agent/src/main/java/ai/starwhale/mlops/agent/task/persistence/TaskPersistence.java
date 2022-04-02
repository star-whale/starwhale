/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

import ai.starwhale.mlops.agent.task.EvaluationTask;
import java.util.List;

/**
 * agent task persistence api
 */
public interface TaskPersistence {

    /**
     * get all tasks
     * @return all tasks
     */
    List<EvaluationTask> getAllActiveTasks() throws Exception;

    /**
     * get task by id
     * @param id key
     * @return task
     */
    EvaluationTask getTaskById(Long id) throws Exception;

    /**
     * get task container status by id
     * @param id key
     * @return task
     */
    ExecuteStatus status(Long id) throws Exception;

    /**
     * "created""running""paused""restarting""removing""exited""dead"
     */
    enum ExecuteStatus {
        /**
         * normal life cycle
         */
        START, RUNNING, OK, FAILED, UNKNOWN
    }
    /**
     * save task
     * @param task task
     * @return if success
     */
    boolean save(EvaluationTask task) throws Exception;

    /**
     * move task to the archived state
     * @param task task
     * @return if success
     */
    boolean move2Archived(EvaluationTask task);

    /**
     * preloading task's swmp tar,and untar it to the dir
     * @param task task
     * @return disk dir path
     */
    String preloadingSWMP(EvaluationTask task) throws Exception;
}
