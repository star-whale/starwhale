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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateController.InstanceType;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * a test for RollingUpdateController
 */
public class RollingUpdateControllerTest {

    @ParameterizedTest
    @EnumSource(value = ServerInstanceStatus.class, names = {"BORN", "READY_DOWN", "READY_UP", "DOWN"})
    public void testInstanceStatusNew(ServerInstanceStatus status) throws Throwable {
        RollingUpdateStatusListeners rollingUpdateStatusListeners = mock(RollingUpdateStatusListeners.class);
        RollingUpdateController rollingUpdateController = new RollingUpdateController(rollingUpdateStatusListeners);
        rollingUpdateController.instanceStatus(status, InstanceType.NEW);
        verify(rollingUpdateStatusListeners).onNewInstanceStatus(status);
    }

    @ParameterizedTest
    @EnumSource(value = ServerInstanceStatus.class, names = {"BORN", "READY_DOWN", "READY_UP", "DOWN"})
    public void testInstanceStatusOld(ServerInstanceStatus status) throws Throwable {
        RollingUpdateStatusListeners rollingUpdateStatusListeners = mock(RollingUpdateStatusListeners.class);
        RollingUpdateController rollingUpdateController = new RollingUpdateController(rollingUpdateStatusListeners);
        rollingUpdateController.instanceStatus(status, InstanceType.OLD);
        verify(rollingUpdateStatusListeners).onOldInstanceStatus(status);
    }
}
