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

package ai.starwhale.mlops.domain.upgrade.rollup;


/**
 * Components that doesn't tolerant multiple server instances shall implement this interface
 */
public interface RollingUpdateStatusListener {

    /**
     * new instance / old instance
     *     BORN ->
     *          <- READY_DOWN
     * READY_UP ->
     *          <- DOWN
     */
    enum ServerInstanceStatus {
        /**
         * application start up but no service is not available yet
         */
        BORN,
        /**
         * All necessary stuffs are done before the instance goes down. Such as:
         * 1. states writing operations are forbidden
         * 2. states have been durably written to the storage
         */
        READY_DOWN,
        /**
         * All necessary stuffs are done before the instance goes up. Such as:
         * 1. storage schemas have been merged
         * 2. states have been loaded from the storage.
         */
        READY_UP,
        /**
         * all services are closed
         */
        DOWN
    }

    /**
     * When this method is called, the current instance is considered to be an old instance which needs to be replaced.
     * The old instance shall do the corresponding stuffs to make sure service works as expected
     * The implementation for this method shall be idempotent
     * Once the method returns, the operation for the old instance is considered to be done
     * So it's necessary that the implementation for this method shall be blocking
     *
     * @param status the status for the new instance
     */
    void onNewInstanceStatus(ServerInstanceStatus status) throws InterruptedException;

    /**
     * When this method is called, the current instance is considered to be the new instance
     * which is going to take over the old one
     * The new instance shall do the corresponding stuffs to make sure service works as expected
     * The implementation for this method shall be idempotent
     * Once the method returns, the operation for the new instance is considered to be done
     * So it's necessary that the implementation for this method shall be blocking
     *
     * @param status the status for the old instance
     */
    void onOldInstanceStatus(ServerInstanceStatus status);
}
