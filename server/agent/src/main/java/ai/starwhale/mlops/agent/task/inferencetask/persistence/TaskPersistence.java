/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
