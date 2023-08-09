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

import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import com.github.dockerjava.api.model.Container;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ContainerStatusExplainer {

    static final Map<String, TaskStatus> STATUS_MAP = new HashMap<>() {
        {
            put("running", TaskStatus.RUNNING);
            put("created", TaskStatus.PREPARING);
            put("dead", TaskStatus.FAIL);
            put("paused", TaskStatus.RUNNING);
            put("restarting", TaskStatus.RUNNING);
        }
    };
    final TaskMapper taskMapper;

    public ContainerStatusExplainer(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public TaskStatus statusOf(Container c, Long taskId) {
        TaskEntity task = taskMapper.findTaskById(taskId);
        String state = c.getState();
        if (null != task && Set.of(TaskStatus.CANCELED, TaskStatus.CANCELLING).contains(task.getTaskStatus())) {
            if ("exited".equalsIgnoreCase(state) || "dead".equalsIgnoreCase(state)) {
                return TaskStatus.CANCELED;
            } else if (STATUS_MAP.containsKey(state)) {
                return TaskStatus.CANCELLING;
            }
            return TaskStatus.UNKNOWN;
        }
        for (var entry : STATUS_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(state)) {
                return entry.getValue();
            }
        }
        if ("exited".equalsIgnoreCase(state)) {
            if (c.getStatus().toUpperCase().contains("Exited (0)".toUpperCase())) {
                return TaskStatus.SUCCESS;
            }
            return TaskStatus.FAIL;
        }

        log.warn("unexpected docker state detected State:{} Status: {}", state, c.getStatus());
        return TaskStatus.UNKNOWN;
    }

}
