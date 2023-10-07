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

package ai.starwhale.mlops.schedule.impl.docker.reporting;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.github.dockerjava.api.model.Container;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ContainerStatusExplainer {

    static final Map<String, RunStatus> STATUS_MAP = new HashMap<>() {
        {
            put("running", RunStatus.RUNNING);
            put("created", RunStatus.PENDING);
            put("dead", RunStatus.FAILED);
            put("paused", RunStatus.RUNNING);
            put("restarting", RunStatus.RUNNING);
        }
    };

    public RunStatus statusOf(Container c) {
        String state = c.getState();
        for (var entry : STATUS_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(state)) {
                return entry.getValue();
            }
        }
        if ("exited".equalsIgnoreCase(state)) {
            if (c.getStatus().toUpperCase().contains("Exited (0)".toUpperCase())) {
                return RunStatus.FINISHED;
            }
            return RunStatus.FAILED;
        }
        log.warn("unexpected docker state detected State:{} Status: {}", state, c.getStatus());
        throw new SwValidationException(
                ValidSubject.TASK,
                MessageFormat.format(
                        "unexpected docker state detected State:{0} Status: {1}",
                        state,
                        c.getStatus()
                )
        );

    }

}
