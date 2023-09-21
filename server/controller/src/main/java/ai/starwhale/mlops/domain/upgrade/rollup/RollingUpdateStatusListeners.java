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
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;


@Component
public class RollingUpdateStatusListeners {

    private final List<RollingUpdateStatusListener> rollingUpdateStatusListeners;

    public RollingUpdateStatusListeners(List<RollingUpdateStatusListener> rollingUpdateStatusListeners) {
        this.rollingUpdateStatusListeners = rollingUpdateStatusListeners.stream().sorted((a, b) -> {
            if (a instanceof OrderedRollingUpdateStatusListener && b instanceof OrderedRollingUpdateStatusListener) {
                return Integer.compare(
                        ((OrderedRollingUpdateStatusListener) a).getOrder(),
                        ((OrderedRollingUpdateStatusListener) b).getOrder()
                );
            } else if (a instanceof OrderedRollingUpdateStatusListener) {
                return -1;
            } else if (b instanceof OrderedRollingUpdateStatusListener) {
                return 1;
            } else {
                return 0;
            }
        }).collect(Collectors.toList());
    }

    void onNewInstanceStatus(ServerInstanceStatus status) {
        for (var listener : rollingUpdateStatusListeners) {
            listener.onNewInstanceStatus(status);
        }
    }

    void onOldInstanceStatus(ServerInstanceStatus status) {
        for (var listener : rollingUpdateStatusListeners) {
            listener.onOldInstanceStatus(status);
        }
    }
}
