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
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.Job;
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
public class JobConverter {

    private final IdConverter idConvertor;
    private final RuntimeService runtimeService;
    private final DatasetDao datasetDao;
    private final SystemSettingService systemSettingService;

    public JobConverter(IdConverter idConvertor,
            RuntimeService runtimeService, DatasetDao datasetDao,
            SystemSettingService systemSettingService) {
        this.idConvertor = idConvertor;
        this.runtimeService = runtimeService;
        this.datasetDao = datasetDao;
        this.systemSettingService = systemSettingService;
    }

    public JobVo convert(Job job) throws ConvertException {
        List<RuntimeVo> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(
                List.of(job.getJobRuntime().getId()));
        if (CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and runtime");
        }
        List<DatasetVersion> datasetVersions = datasetDao.listDatasetVersionsOfJob(job.getId());

        List<String> idList = datasetVersions.stream()
                .map(DatasetVersion::getVersionName)
                .collect(Collectors.toList());

        return JobVo.builder()
                .id(idConvertor.convert(job.getId()))
                .uuid(job.getUuid())
                .owner(UserVo.from(job.getOwner(), idConvertor))
                .modelName(job.getModel().getName())
                .modelVersion(job.getModel().getVersion())
                .createdTime(job.getCreatedTime().getTime())
                .runtime(runtimeByVersionIds.get(0))
                .datasets(idList)
                .jobStatus(job.getStatus())
                .stopTime(job.getFinishedTime().getTime())
                .duration(job.getDurationMs())
                .comment(job.getComment())
                .resourcePool(job.getResourcePool().getName())
                .pinned(job.isPinned())
                .build();
    }

    public JobVo convert(JobEntity jobEntity) throws ConvertException {
        List<RuntimeVo> runtimeByVersionIds = runtimeService.findRuntimeByVersionIds(
                List.of(jobEntity.getRuntimeVersionId()));
        if (CollectionUtils.isEmpty(runtimeByVersionIds) || runtimeByVersionIds.size() > 1) {
            throw new SwProcessException(ErrorType.SYSTEM, "data not consistent between job and runtime");
        }
        List<DatasetVersion> datasetVersions = datasetDao.listDatasetVersionsOfJob(jobEntity.getId());

        List<String> idList = datasetVersions.stream()
                .map(DatasetVersion::getVersionName)
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
                .duration(jobEntity.getDurationMs())
                .comment(jobEntity.getComment())
                .resourcePool(systemSettingService.queryResourcePool(jobEntity.getResourcePool()).getName())
                .pinned(job.isPinned())
                .build();
    }

}
