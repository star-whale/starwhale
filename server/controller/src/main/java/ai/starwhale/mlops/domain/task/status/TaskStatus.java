/*
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

package ai.starwhale.mlops.domain.task.status;

/**
 * possible statuses of a task
 */
public enum TaskStatus {

    /**
     * after created before assigned to an Agent. Ready to be scheduled
     */
    CREATED,

    /**
     * after created before assigned to an Agent. Ready to be scheduled
     */
    ASSIGNING,

    /**
     * pausing triggered by user
     */
    PAUSED,

    /**
     * after assignment is acknowledged before running
     */
    PREPARING,

    /**
     * running
     */
    RUNNING,

    /**
     * garbage is cleared task is finished
     */
    SUCCESS,

    /**
     * cancel triggered by the user
     */
    TO_CANCEL,

    /**
     * agent cancelling task
     */
    CANCELLING,

    /**
     * task canceled success by agent
     */
    CANCELED,

    /**
     * task exit with unexpected error
     */
    FAIL,

    /**
     * UNKNOWN from an Integer
     */
    UNKNOWN();

}
