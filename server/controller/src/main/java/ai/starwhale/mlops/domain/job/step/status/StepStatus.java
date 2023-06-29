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

package ai.starwhale.mlops.domain.job.step.status;

public enum StepStatus {
    /**
     * created by user
     */
    CREATED(),

    /**
     * ready to be scheduled
     */
    READY(),

    /**
     * paused by user
     */
    PAUSED(),

    /**
     * scheduling
     */
    RUNNING(),

    /**
     * CANCEL triggered by user( at least one task is TO_CANCEL)
     */
    TO_CANCEL(),

    /**
     * CANCEL triggered by user( at least one task is TO_CANCEL)
     */
    CANCELLING(),

    /**
     * CANCELLING is done
     */
    CANCELED(),

    /**
     * all the tasks are finished
     */
    SUCCESS(),

    /**
     * some task exit with unexpected error
     */
    FAIL(),

    UNKNOWN();
}
