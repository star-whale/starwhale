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
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.Converter;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.job.mapper.JobDatasetVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.exception.ConvertException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class JobConverter implements Converter<JobEntity, JobVo> {

    private final IdConverter idConvertor;
    private final RuntimeService runtimeService;
    private final JobDatasetVersionMapper jobDatasetVersionMapper;
    private final SystemSettingService systemSettingService;

    public JobConverter(IdConverter idConvertor,
            RuntimeService runtimeService, JobDatasetVersionMapper jobDatasetVersionMapper,
            SystemSettingService systemSettingService) {
        this.idConvertor = idConvertor;
        this.runtimeService = runtimeService;
        this.jobDatasetVersionMapper = jobDatasetVersionMapper;
        this.systemSettingService = systemSettingService;
    }

    @Override
    public JobVo convert(JobEntity jobEntity) throws ConvertException {
        List<RuntimeVo> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(
                List.of(jobEntity.getRuntimeVersionId()));
        if (CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and runtime");
        }
        List<DatasetVersionEntity> dsvEntities = jobDatasetVersionMapper.listDatasetVersionsByJobId(
                jobEntity.getId());

        List<String> idList = dsvEntities.stream()
                .map(DatasetVersionEntity::getVersionName)
                .collect(Collectors.toList());

        return JobVo.builder()
                .id(idConvertor.convert(jobEntity.getId()))
                .uuid(jobEntity.getJobUuid())
                .owner(UserVo.fromEntity(jobEntity.getOwner(), idConvertor))
                .modelName(jobEntity.getModelName())
                .modelVersion(jobEntity.getModelVersion().getVersionName())
                .createdTime(jobEntity.getCreatedTime().getTime())
                .runtime(runtimeByVersionIds.get(0))
                .datasets(idList)
                .jobStatus(jobEntity.getJobStatus())
                .stopTime(jobEntity.getFinishedTime().getTime())
                .comment(jobEntity.getComment())
                .resourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()).getName())
                .build();
    }

    @Override
    public JobEntity revert(JobVo jobVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }

}
