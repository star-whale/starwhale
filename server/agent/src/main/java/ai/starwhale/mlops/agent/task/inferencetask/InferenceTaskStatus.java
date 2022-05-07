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

package ai.starwhale.mlops.agent.task.inferencetask;

/**
 * status of a task
 */
public enum InferenceTaskStatus {

    /**
     * after assignment is acknowledged before running
     */
    PREPARING,

    /**
     * running
     */
    RUNNING,

    /**
     * after task exit normally(container is stopped)
     */
    UPLOADING,

    /**
     *
     */
    SUCCESS,

    /**
     * task exit with unexpected error
     */
    FAIL,

    /**
     * canceling triggered by the user
     */
    CANCELING,

    /**
     * task canceled success by agent
     */
    CANCELED,

    /**
     * when report successfully to the controller,it should be archived (Agent only status)
     */
    ARCHIVED

}
