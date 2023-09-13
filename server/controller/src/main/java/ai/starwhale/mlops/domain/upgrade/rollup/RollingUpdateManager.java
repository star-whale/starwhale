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

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(2)
public class RollingUpdateManager implements CommandLineRunner {

    private final List<RollingUpdateStatusListener> rollingUpdateStatusListeners;

    private final Boolean rollUp;

    public RollingUpdateManager(
            List<RollingUpdateStatusListener> rollingUpdateStatusListeners,
            @Value("${sw.rollup}") Boolean rollUp
    ) {
        this.rollingUpdateStatusListeners = rollingUpdateStatusListeners;
        this.rollUp = rollUp;
    }

    @Override
    public void run(String... args) throws Exception {

        if (!rollUp) {
            log.info("start up in normal start mode ...");
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
            }
            for (var l : rollingUpdateStatusListeners) {
                l.onOldInstanceStatus(ServerInstanceStatus.DOWN);
            }
        } else {
            log.info("start up in rolling update mode, waiting for old controller instance status notify ...");
        }
    }

}
