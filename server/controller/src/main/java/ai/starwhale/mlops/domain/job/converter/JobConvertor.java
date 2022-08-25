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

import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.system.mapper.ResourcePoolMapper;
import ai.starwhale.mlops.domain.system.po.ResourcePoolEntity;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolConverter;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class JobConvertor implements Convertor<JobEntity, JobVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private JobSWDSVersionMapper jobSWDSVersionMapper;

    @Resource
    private ResourcePoolMapper resourcePoolMapper;

    @Resource
    private ResourcePoolConverter resourcePoolConverter;

    @Override
    public JobVO convert(JobEntity jobEntity) throws ConvertException {
        List<RuntimeVO> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(
            List.of(jobEntity.getRuntimeVersionId()));
        if(CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size()>1){
            throw new SWProcessException(ErrorType.SYSTEM).tip("data not consistent between job and runtime ");
        }
        List<SWDatasetVersionEntity> dsvEntities = jobSWDSVersionMapper.listSWDSVersionsByJobId(
            jobEntity.getId());

        List<String> idList = dsvEntities.stream()
            .map(SWDatasetVersionEntity::getVersionName)
            .collect(Collectors.toList());

        ResourcePoolEntity resourcePoolEntity = resourcePoolMapper.findById(jobEntity.getResourcePoolId());
        var resourcePool = resourcePoolConverter.toResourcePool(resourcePoolEntity);

        return JobVO.builder()
            .id(idConvertor.convert(jobEntity.getId()))
            .uuid(jobEntity.getJobUuid())
            .owner(userConvertor.convert(jobEntity.getOwner()))
            .modelName(jobEntity.getModelName())
            .modelVersion(jobEntity.getSwmpVersion().getVersionName())
            .createdTime(localDateTimeConvertor.convert(jobEntity.getCreatedTime()))
            .runtime(runtimeByVersionIds.get(0))
            .datasets(idList)
            .device(getDeviceName(jobEntity.getDeviceType()))
            .deviceAmount(jobEntity.getDeviceAmount())
            .jobStatus(jobEntity.getJobStatus())
            .stopTime(localDateTimeConvertor.convert(jobEntity.getFinishedTime()))
            .comment(jobEntity.getComment())
            .resourcePool(resourcePool.getLabel())
            .build();
    }

    @Override
    public JobEntity revert(JobVO jobVO) throws ConvertException {
        Objects.requireNonNull(jobVO, "jobVO");
        return null;
    }

    private String getDeviceName(int deviceType) {
        if(deviceType > 0 && deviceType <= Clazz.values().length) {
            return Device.Clazz.values()[deviceType - 1].name();
        }
        return "UNDEFINED";
    }
}
