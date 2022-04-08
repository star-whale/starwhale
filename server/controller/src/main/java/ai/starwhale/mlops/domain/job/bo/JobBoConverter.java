/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job.bo;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.job.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * convert JobEntity to Job
 */
@Component
public class JobBoConverter {

    final JobSWDSVersionMapper jobSWDSVersionMapper;

    public JobBoConverter(JobSWDSVersionMapper jobSWDSVersionMapper) {
        this.jobSWDSVersionMapper = jobSWDSVersionMapper;
    }

    public Job fromEntity(JobEntity jobEntity){
        List<SWDataSet> swDataSets = jobSWDSVersionMapper.listSWDSVersionsByJobId(jobEntity.getId()).stream().map(swDatasetVersionEntity -> SWDataSet.builder().id(swDatasetVersionEntity.getId())
                .indexPath(getIndexPath(swDatasetVersionEntity))
                .path(swDatasetVersionEntity.getStoragePath())
                .build()).collect(Collectors.toList());
        return Job.builder()
            .id(jobEntity.getId())
            .jobRuntime(JobRuntime.builder().baseImage(jobEntity.getBaseImage().getImageName()).deviceAmount(jobEntity.getDeviceAmount()).deviceClass(
                Device.Clazz.from(jobEntity.getDeviceType())).build())
            .status(JobStatus.from(jobEntity.getJobStatus()))
            .swmp(SWModelPackage
                .builder().id(jobEntity.getSwmpVersionId()).path(jobEntity.getSwmpVersion().getStoragePath()).build())
            .swDataSets(swDataSets)
            .build();
    }

    static final String PATH_INDEX = "index.jsonl";
    private String getIndexPath(SWDatasetVersionEntity swDatasetVersionEntity) {
        return swDatasetVersionEntity.getStoragePath() + PATH_INDEX;
    }


}
