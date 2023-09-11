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
 * components that doesn't tolerant multiple server instances shall implement this interface
 */
public interface RollingUpdateStatusListener {

    /**
     * new instance / old instance
     * READY_UP ->
     *          <- READY_DOWN
     *       UP ->
     *          <- DOWN
     */
    enum ServerInstanceStatus {
        READY_UP, UP, READY_DOWN, DOWN
    }

    /**
     * when this method is called, the current instance is considered to be an old instance
     * which needs to be replaced
     * The old instance shall do the corresponding things to make sure service works as expected
     * The implementation for this method shall be idempotent
     *
     * @param status the status for the new instance
     */
    void onNewInstanceStatus(ServerInstanceStatus status) throws InterruptedException;

    /**
     * when this method is called, the current instance is considered to be a new instance
     * which is ready to take over the old one
     * The implementation for this method shall be idempotent
     *
     * @param status the status for the old instace
     */
    void onOldInstanceStatus(ServerInstanceStatus status);
}
