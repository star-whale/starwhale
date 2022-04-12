/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

import ai.starwhale.mlops.agent.task.EvaluationTask;
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
    Optional<List<EvaluationTask>> getAllActiveTasks();

    /**
     * get task by id
     * @param id key
     * @return task
     */
    Optional<EvaluationTask> getTaskById(Long id);

    /**
     * get task container status by id
     * @param id key
     * @return task
     */
    Optional<ExecuteStatus> status(Long id);

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
    boolean save(EvaluationTask task);

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
    boolean preloadingSWMP(EvaluationTask task);

    boolean generateSWDSConfig(EvaluationTask task);

    /**
     * upload result to storage
     * @param task task
     * @return oss path
     */
    boolean uploadResult(EvaluationTask task);

    /**
     * @param id taskId
     * taskInfo dir path,Eg:/var/starwhale/task/{taskId}/taskInfo.json(format:json)
     */
    String pathOfInfoFile(Long id);

    /**
     * @param id taskId
     * task running status dir path,Eg:/var/starwhale/task/{taskId}/status/current(format:txt)
     */
    String pathOfStatusFile(Long id);

    /**
     * @param id taskId
     * one task's base dir path,Eg:/var/starwhale/task/{taskId}/
     */
    String basePathOfTask(Long id);

    /**
     * @param id taskId
     * swmp dir path,Eg:/var/starwhale/task/{taskId}/swmp/(dir)
     */
    String pathOfSWMPDir(Long id);

    /**
     * @param id taskId
     * swds config file path,Eg:/var/starwhale/task/{taskId}/config/swds.json(format:json)
     */
    String pathOfSWDSFile(Long id);

    /**
     * @param id taskId
     * task result dir path,Eg:/var/starwhale/task/{taskId}/result/
     */
    String pathOfResult(Long id);

    /**
     * task runtime log dir path,Eg:/var/starwhale/task/log/{taskId}/log
     */
    String pathOfLog(Long id);
}
