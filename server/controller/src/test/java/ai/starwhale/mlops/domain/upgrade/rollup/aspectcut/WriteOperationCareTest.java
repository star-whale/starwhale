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

package ai.starwhale.mlops.domain.upgrade.rollup.aspectcut;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import ai.starwhale.mlops.exception.SwUnavailableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for WriteOperationCare
 */
public class WriteOperationCareTest {

    @Test
    public void testNewInstanceBorn() throws Throwable {
        WriteOperationCare writeOperationCare = new WriteOperationCare();
        writeOperationCare.onOldInstanceStatus(ServerInstanceStatus.DOWN);
        writeOperationCare.writeCare(null);
        writeOperationCare.onNewInstanceStatus(ServerInstanceStatus.BORN);
        Assertions.assertThrows(SwUnavailableException.class, () -> writeOperationCare.writeCare(null));
    }

    @Test
    public void testNewInstanceDown() throws Throwable {
        WriteOperationCare writeOperationCare = new WriteOperationCare();
        Assertions.assertThrows(SwUnavailableException.class, () -> writeOperationCare.writeCare(null));
        writeOperationCare.onNewInstanceStatus(ServerInstanceStatus.DOWN);
        writeOperationCare.writeCare(null);
    }

    @Test
    public void testOldInstanceDown() throws Throwable {
        WriteOperationCare writeOperationCare = new WriteOperationCare();
        Assertions.assertThrows(SwUnavailableException.class, () -> writeOperationCare.writeCare(null));
        writeOperationCare.onOldInstanceStatus(ServerInstanceStatus.DOWN);
        writeOperationCare.writeCare(null);
    }
}
