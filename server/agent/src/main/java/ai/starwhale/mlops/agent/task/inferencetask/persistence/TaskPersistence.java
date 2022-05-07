/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.persistence;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;

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
    Optional<List<InferenceTask>> getAllActiveTasks();

    /**
     * get task by id
     * @param id key
     * @return task
     */
    Optional<InferenceTask> getActiveTaskById(Long id);

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
    boolean save(InferenceTask task);

    /**
     * preloading task's swmp tar,and untar it to the dir
     * @param task task
     * @return disk dir path
     */
    void preloadingSWMP(InferenceTask task) throws Exception;

    /**
     * pre generate config file
     * @param task task
     * @return if success
     */
    void generateConfigFile(InferenceTask task) throws Exception;

    /**
     * upload result to storage
     * @param task task
     * @return oss path
     */
    void uploadResult(InferenceTask task) throws Exception;
    void uploadLog(InferenceTask task) throws Exception;
    void uploadContainerLog(InferenceTask task, String logPath);

}
