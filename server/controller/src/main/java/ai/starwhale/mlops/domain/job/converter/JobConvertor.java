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

package ai.starwhale.mlops.domain.job.converter;

import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.job.mapper.JobSwdsVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolConverter;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class JobConvertor implements Convertor<JobEntity, JobVo> {

    private final IdConvertor idConvertor;
    private final UserConvertor userConvertor;
    private final ResourcePoolConverter resourcePoolConverter;
    private final RuntimeService runtimeService;
    private final JobSwdsVersionMapper jobSwdsVersionMapper;
    private final ResourcePoolMapper resourcePoolMapper;

    public JobConvertor(IdConvertor idConvertor, UserConvertor userConvertor,
            ResourcePoolConverter resourcePoolConverter,
            RuntimeService runtimeService, JobSwdsVersionMapper jobSwdsVersionMapper,
            ResourcePoolMapper resourcePoolMapper) {
        this.idConvertor = idConvertor;
        this.userConvertor = userConvertor;
        this.resourcePoolConverter = resourcePoolConverter;
        this.runtimeService = runtimeService;
        this.jobSwdsVersionMapper = jobSwdsVersionMapper;
        this.resourcePoolMapper = resourcePoolMapper;
    }

    @Override
    public JobVo convert(JobEntity jobEntity) throws ConvertException {
        List<RuntimeVo> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(
                List.of(jobEntity.getRuntimeVersionId()));
        if (CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM).tip("data not consistent between job and runtime ");
        }
        List<SwDatasetVersionEntity> dsvEntities = jobSwdsVersionMapper.listSwdsVersionsByJobId(
                jobEntity.getId());

        List<String> idList = dsvEntities.stream()
                .map(SwDatasetVersionEntity::getVersionName)
                .collect(Collectors.toList());

        ResourcePoolEntity resourcePoolEntity = resourcePoolMapper.findById(jobEntity.getResourcePoolId());
        var resourcePool = resourcePoolConverter.toResourcePool(resourcePoolEntity);

        return JobVo.builder()
                .id(idConvertor.convert(jobEntity.getId()))
                .uuid(jobEntity.getJobUuid())
                .owner(userConvertor.convert(jobEntity.getOwner()))
                .modelName(jobEntity.getModelName())
                .modelVersion(jobEntity.getSwmpVersion().getVersionName())
                .createdTime(jobEntity.getCreatedTime().getTime())
                .runtime(runtimeByVersionIds.get(0))
                .datasets(idList)
                .device(getDeviceName(jobEntity.getDeviceType()))
                .deviceAmount(jobEntity.getDeviceAmount())
                .jobStatus(jobEntity.getJobStatus())
                .stopTime(jobEntity.getFinishedTime().getTime())
                .comment(jobEntity.getComment())
                .resourcePool(resourcePool.getLabel())
                .build();
    }

    @Override
    public JobEntity revert(JobVo jobVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }

    private String getDeviceName(int deviceType) {
        if (deviceType > 0 && deviceType <= Clazz.values().length) {
            return Device.Clazz.values()[deviceType - 1].name();
        }
        return "UNDEFINED";
    }
}
