/**
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

package ai.starwhale.mlops.domain.job.bo;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.swmp.SWModelPackageEntity;
import ai.starwhale.mlops.domain.swmp.mapper.SWModelPackageMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * convert JobEntity to Job
 */
@Component
public class JobBoConverter {

    final JobSWDSVersionMapper jobSWDSVersionMapper;

    final SWModelPackageMapper swModelPackageMapper;

    public JobBoConverter(JobSWDSVersionMapper jobSWDSVersionMapper,
        SWModelPackageMapper swModelPackageMapper) {
        this.jobSWDSVersionMapper = jobSWDSVersionMapper;
        this.swModelPackageMapper = swModelPackageMapper;
    }

    public Job fromEntity(JobEntity jobEntity){
        List<SWDataSet> swDataSets = jobSWDSVersionMapper.listSWDSVersionsByJobId(jobEntity.getId())
            .stream().map(swDatasetVersionEntity -> SWDataSet.builder()
                .id(swDatasetVersionEntity.getId())
                .indexPath(getIndexPath(swDatasetVersionEntity))
                .path(swDatasetVersionEntity.getStoragePath())
                .version(swDatasetVersionEntity.getVersionName())
                .name(swDatasetVersionEntity.getDatasetName())
                .build())
            .collect(Collectors.toList());
        SWModelPackageEntity modelPackageEntity = swModelPackageMapper.findSWModelPackageById(
            jobEntity.getSwmpVersion().getSwmpId());
        return Job.builder()
            .id(jobEntity.getId())
            .jobRuntime(JobRuntime.builder().baseImage(jobEntity.getBaseImage().getImageName()).deviceAmount(jobEntity.getDeviceAmount()).deviceClass(
                Device.Clazz.from(jobEntity.getDeviceType())).build())
            .status(jobEntity.getJobStatus())
            .swmp(SWModelPackage
                .builder()
                .id(jobEntity.getSwmpVersionId())
                .name(modelPackageEntity.getSwmpName())
                .version(jobEntity.getSwmpVersion().getVersionName())
                .path(jobEntity.getSwmpVersion().getStoragePath()).build())
            .swDataSets(swDataSets)
            .resultDir(jobEntity.getResultOutputPath())
            .uuid(jobEntity.getJobUuid())
            .build();
    }

    static final String PATH_INDEX = "/index.jsonl";
    private String getIndexPath(SWDatasetVersionEntity swDatasetVersionEntity) {
        return swDatasetVersionEntity.getStoragePath() + PATH_INDEX;
    }


}
