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

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import io.vavr.Tuple2;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class UpgradeHealthIndicator implements HealthIndicator, RollingUpdateStatusListener {

    private volatile boolean primaryInstance;
    private final Map<String, List<Tuple2>> details = new HashMap<>() {
        {
            put(DETAIL_NEW_INSTANCE_STATUSES, Collections.synchronizedList(new LinkedList<>()));
            put(DETAIL_OLD_INSTANCE_STATUSES, Collections.synchronizedList(new LinkedList<>()));
        }
    };
    private static final String DETAIL_NEW_INSTANCE_STATUSES = "newInstanceStatuses";
    private static final String DETAIL_OLD_INSTANCE_STATUSES = "oldInstanceStatuses";

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        if (primaryInstance) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }

    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) throws InterruptedException {
        details.get(DETAIL_NEW_INSTANCE_STATUSES).add(new Tuple2<>(System.currentTimeMillis(), status));
        if (status == ServerInstanceStatus.READY_UP) {
            primaryInstance = false;
        } else if (status == ServerInstanceStatus.DOWN) {
            primaryInstance = true;
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        details.get(DETAIL_OLD_INSTANCE_STATUSES).add(new Tuple2<>(System.currentTimeMillis(), status));
        if (status == ServerInstanceStatus.DOWN) {
            primaryInstance = true;
        }
    }
}
