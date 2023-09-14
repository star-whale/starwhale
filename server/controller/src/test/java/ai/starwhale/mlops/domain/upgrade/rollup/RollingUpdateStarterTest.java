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


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * a test for RollingUpdateManager
 */
public class RollingUpdateStarterTest {

    @Test
    public void testRollingUpdate() throws Throwable {
        RollingUpdateStatusListener listener = mock(RollingUpdateStatusListener.class);
        RollingUpdateStarter rollingUpdateStarter = new RollingUpdateStarter(new RollingUpdateStatusListeners(List.of(
                listener)), true);
        rollingUpdateStarter.run();
        verify(listener, times(0)).onOldInstanceStatus(any());
    }

    @Test
    public void testNormal() throws Throwable {
        RollingUpdateStatusListener listener = mock(RollingUpdateStatusListener.class);
        RollingUpdateStarter rollingUpdateStarter = new RollingUpdateStarter(new RollingUpdateStatusListeners(List.of(
                listener)), false);
        rollingUpdateStarter.run();
        verify(listener, times(1)).onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
        verify(listener, times(1)).onOldInstanceStatus(ServerInstanceStatus.DOWN);
    }
}
