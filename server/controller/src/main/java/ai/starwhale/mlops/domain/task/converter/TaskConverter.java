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

package ai.starwhale.mlops.domain.task.converter;

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TaskConverter {

    private final IdConverter idConvertor;

    private final StepMapper stepMapper;

    public TaskConverter(IdConverter idConvertor, StepMapper stepMapper) {
        this.idConvertor = idConvertor;
        this.stepMapper = stepMapper;
    }

    public TaskVo convert(TaskEntity entity) {
        if (entity == null) {
            return null;
        }
        StepEntity step = stepMapper.findById(entity.getStepId());
        if (null == step) {
            throw new SwProcessException(ErrorType.DB,
                    String.format("bad task data: no step for task %s", entity.getId()));
        }
        var pool = "";
        if (StringUtils.hasText(step.getPoolInfo())) {
            try {
                pool = ResourcePool.fromJson(step.getPoolInfo()).getName();
            } catch (Exception e) {
                throw new SwProcessException(ErrorType.DB,
                        String.format("bad task data: can not unmarshal pool %s", entity.getId()), e);
            }
        }

        return TaskVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .uuid(entity.getTaskUuid())
                .taskStatus(entity.getTaskStatus())
                .retryNum(entity.getRetryNum())
                .endTime(entity.getFinishedTime().getTime())
                .stepName(step.getName())
                .createdTime(entity.getStartedTime().getTime())
                .resourcePool(pool)
                .build();
    }

}
