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

package ai.starwhale.mlops.domain.job.step.task.converter;


import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.task.po.TaskEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TaskConverter {

    private final IdConverter idConvertor;
    // TODO shouldn't use mapper here
    private final StepMapper stepMapper;
    private final int devPort;
    private final WebServerInTask webServerInTask;


    public TaskConverter(
            IdConverter idConvertor, StepMapper stepMapper,
            @Value("${sw.task.dev-port}") int devPort,
            WebServerInTask webServerInTask
    ) {
        this.idConvertor = idConvertor;
        this.stepMapper = stepMapper;
        this.devPort = devPort;
        this.webServerInTask = webServerInTask;
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

        String devUrl = null;
        if (entity.getDevWay() != null && StringUtils.hasText(entity.getIp())) {
            devUrl = webServerInTask.generateGatewayUrl(entity.getId(), entity.getIp(), devPort);
        }

        return TaskVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .uuid(entity.getTaskUuid())
                .taskStatus(entity.getTaskStatus())
                .retryNum(entity.getRetryNum())
                .finishedTime(entity.getFinishedTime() == null ? null : entity.getFinishedTime().getTime())
                .stepName(step.getName())
                .devUrl(devUrl)
                .startedTime(entity.getStartedTime() == null ? null : entity.getStartedTime().getTime())
                .resourcePool(pool)
                .failedReason(entity.getFailedReason())
                .build();
    }

}