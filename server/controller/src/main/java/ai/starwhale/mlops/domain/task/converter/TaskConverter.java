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


import ai.starwhale.mlops.api.protobuf.Job.ExposedLinkVo;
import ai.starwhale.mlops.api.protobuf.Job.ExposedType;
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TaskConverter {

    private final IdConverter idConvertor;
    private final StepMapper stepMapper;
    private final int devPort;
    private final WebServerInTask webServerInTask;
    private final JobSpecParser jobSpecParser;


    public TaskConverter(
            IdConverter idConvertor, StepMapper stepMapper,
            @Value("${sw.task.dev-port}") int devPort,
            WebServerInTask webServerInTask,
            JobSpecParser jobSpecParser
    ) {
        this.idConvertor = idConvertor;
        this.stepMapper = stepMapper;
        this.devPort = devPort;
        this.webServerInTask = webServerInTask;
        this.jobSpecParser = jobSpecParser;
    }

    public TaskVo convert(TaskEntity entity) {
        if (entity == null) {
            return null;
        }
        StepEntity stepEntity = stepMapper.findById(entity.getStepId());
        if (null == stepEntity) {
            throw new SwProcessException(ErrorType.DB,
                    String.format("bad task data: no step for task %s", entity.getId()));
        }
        var pool = "";
        if (StringUtils.hasText(stepEntity.getPoolInfo())) {
            try {
                pool = ResourcePool.fromJson(stepEntity.getPoolInfo()).getName();
            } catch (Exception e) {
                throw new SwProcessException(ErrorType.DB,
                        String.format("bad task data: can not unmarshal pool %s", entity.getId()), e);
            }
        }

        var exposed = new ArrayList<ExposedLinkVo>();
        var ip = entity.getIp();
        if (TaskStatus.RUNNING.equals(entity.getTaskStatus()) && StringUtils.hasText(ip)) {
            if (entity.getDevWay() != null) {
                var devUrl = webServerInTask.generateGatewayUrl(entity.getId(), ip, devPort);
                exposed.add(ExposedLinkVo.newBuilder()
                        .setType(ExposedType.DEV_MODE)
                        .setName(DevWay.VS_CODE.name())
                        .setLink(devUrl)
                        .build());
            }
            var step = jobSpecParser.stepFromJsonQuietly(stepEntity.getOriginJson());
            if (step != null && step.hasExpose() && step.getExpose() > 0) {
                exposed.add(ExposedLinkVo.newBuilder()
                        .setType(ExposedType.WEB_HANDLER)
                        .setName(step.hasShowName()  ? step.getShowName() : step.getName())
                        .setLink(webServerInTask.generateGatewayUrl(entity.getId(), ip, step.getExpose()))
                        .build());
            }
        }

        return TaskVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .uuid(entity.getTaskUuid())
                .taskStatus(entity.getTaskStatus())
                .retryNum(entity.getRetryNum())
                .finishedTime(entity.getFinishedTime() == null ? null : entity.getFinishedTime().getTime())
                .stepName(stepEntity.getName())
                .exposedLinks(exposed.isEmpty() ? null : exposed)
                .startedTime(entity.getStartedTime() == null ? null : entity.getStartedTime().getTime())
                .resourcePool(pool)
                .failedReason(entity.getFailedReason())
                .build();
    }

}
