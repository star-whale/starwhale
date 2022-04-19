/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.ppltask.persistence;

import ai.starwhale.mlops.agent.task.ppltask.PPLTask;

import java.util.List;
import java.util.Optional;

/**
 * agent task persistence api
 */
public interface TaskPersistence {


    /**
     * get all tasks
     * @return all tasks
     */
    Optional<List<PPLTask>> getAllActiveTasks();

    /**
     * get task by id
     * @param id key
     * @return task
     */
    Optional<PPLTask> getTaskById(Long id);

    /**
     * get task container status by id
     * @param id key
     * @return task
     */
    Optional<ExecuteStatus> status(Long id);

    /**
     * update status of task
     * @param id task id
     * @param status status
     * @return if success
     */
    boolean updateStatus(Long id, ExecuteStatus status) throws Exception;

    /**
     * "created""running""paused""restarting""removing""exited""dead"
     */
    enum ExecuteStatus {
        /**
         * normal life cycle
         */
        start, running, success, failed, unknown
    }
    /**
     * save task
     * @param task task
     * @return if success
     */
    boolean save(PPLTask task);

    /**
     * move task to the archived state
     * @param task task
     * @return if success
     */
    void move2Archived(PPLTask task) throws Exception;

    /**
     * preloading task's swmp tar,and untar it to the dir
     * @param task task
     * @return disk dir path
     */
    String preloadingSWMP(PPLTask task) throws Exception;

    /**
     * pre generate swds config
     * @param task task
     * @return if success
     */
    void generateSWDSConfig(PPLTask task) throws Exception;

    /**
     * upload result to storage
     * @param task task
     * @return oss path
     */
    void uploadResult(PPLTask task) throws Exception;

}
