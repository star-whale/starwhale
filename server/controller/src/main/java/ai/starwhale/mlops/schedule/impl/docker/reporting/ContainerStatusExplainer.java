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

import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import com.github.dockerjava.api.model.Container;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;


@Slf4j
public class ContainerStatusExplainer {

    final TaskMapper taskMapper;

    static final Map<String, TaskStatus> STATUS_MAP = new HashMap<>(){{
        put("running", TaskStatus.RUNNING);
        put("created", TaskStatus.PREPARING);
        put("dead", TaskStatus.FAIL);
        put("paused", TaskStatus.RUNNING);
        put("restarting", TaskStatus.RUNNING);
    }};

    public ContainerStatusExplainer(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public TaskStatus statusOf(Container c, Long taskId){
        for(var entry : STATUS_MAP.entrySet()){
            if(entry.getKey().equalsIgnoreCase(c.getState())){
                return entry.getValue();
            }
        }
        if("exited".equalsIgnoreCase(c.getState())){
            TaskEntity task = taskMapper.findTaskById(taskId);
            if(null != task && Set.of(TaskStatus.CANCELED, TaskStatus.CANCELLING).contains(task.getTaskStatus())){
                return  TaskStatus.CANCELED;
            }
            if(c.getStatus().toUpperCase().contains("Exited (0)".toUpperCase())){
                return TaskStatus.SUCCESS;
            }
            return TaskStatus.FAIL;
        }// exited/running/created

        log.warn("unexpected docker state detected State:{} Status: {}", c.getState(), c.getStatus());
        return TaskStatus.UNKNOWN;
    }

}
