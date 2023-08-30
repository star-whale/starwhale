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

package ai.starwhale.mlops.common.proxy;

import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * This class is used to proxy the model serving service.
 * The model serving service uri is like "model-serving/1/xxx", the first number is the id of the model serving entry.
 * The proxy will find the target host by the id.
 * The proxy will update the last visit time of the model serving entry which is used to do the garbage collection.
 */
@Slf4j
@Component
public class ModelServing implements Service {
    private final ModelServingMapper modelServingMapper;

    private final TaskMapper taskMapper;

    private final StepMapper stepMapper;

    public static final String MODEL_SERVICE_PREFIX = "model-serving";

    public ModelServing(ModelServingMapper modelServingMapper, TaskMapper taskMapper, StepMapper stepMapper) {
        this.modelServingMapper = modelServingMapper;
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
    }

    @Override
    public String getPrefix() {
        return MODEL_SERVICE_PREFIX;
    }

    @Override
    public String getTarget(String uri) {
        var parts = uri.split("/", 2);

        var id = Long.parseLong(parts[0]);

        ModelServingEntity modelServingEntity = modelServingMapper.find(id);
        if (modelServingEntity == null) {
            throw new IllegalArgumentException("can not find model serving entry " + parts[1]);
        }
        modelServingMapper.updateLastVisitTime(id, new Date());
        List<StepEntity> steps = stepMapper.findByJobId(modelServingEntity.getJobId());
        if (CollectionUtils.isEmpty(steps)) {
            log.info("steps for job haven't been created yet {}", modelServingEntity.getJobId());
            return null;
        }
        StepEntity stepEntity = steps.get(0);
        List<TaskEntity> tasks = taskMapper.findByStepId(stepEntity.getId());
        if (CollectionUtils.isEmpty(tasks)) {
            log.info("tasks for job haven't been created yet {}", modelServingEntity.getJobId());
            return null;
        }
        TaskEntity taskEntity = tasks.get(0);
        String ip = taskEntity.getIp();
        if (!StringUtils.hasText(ip)) {
            log.info("tasks {} hasn't been assigned to a node yet", taskEntity.getId());
            return null;
        }

        var handler = "";
        if (parts.length == 2) {
            handler = parts[1];
        }
        return String.format("http://%s:%d/%s", ip, 8080, handler);
    }
}
