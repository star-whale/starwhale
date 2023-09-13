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

package ai.starwhale.mlops.domain.upgrade.rollup.web;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * a test for UpgradeHealthIndicator
 */
public class HealthIndicatorTest {

    @Test
    public void testNewInstanceStatus() throws Throwable {
        UpgradeHealthIndicator upgradeHealthIndicator = new UpgradeHealthIndicator();
        upgradeHealthIndicator.onNewInstanceStatus(ServerInstanceStatus.READY_UP);
        Health health = upgradeHealthIndicator.health();
        Assertions.assertEquals(Status.DOWN, health.getStatus());
        upgradeHealthIndicator.onNewInstanceStatus(ServerInstanceStatus.DOWN);
        health = upgradeHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }

    @Test
    public void testOldInstanceStatus() throws Throwable {
        UpgradeHealthIndicator upgradeHealthIndicator = new UpgradeHealthIndicator();
        upgradeHealthIndicator.onOldInstanceStatus(ServerInstanceStatus.DOWN);
        Health health = upgradeHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }
}
