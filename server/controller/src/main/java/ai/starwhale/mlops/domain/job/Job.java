/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    Long id;

    String uuid;

    /**
     * the SWDSs to run on
     */
    List<SWDataSet> swDataSets;

    /**
     * SWMP to be run
     */
    SWModelPackage swmp;

    /**
     * runtime info of the job
     */
    JobRuntime jobRuntime;

    /**
     * the status of a job
     */
    JobStatus status;

    /**
     * evaluation metrics holding dir
     */
    String resultDir;

    public Job deepCopy(){
        return Job.builder()
            .id(this.id)
            .uuid(this.uuid)
            .jobRuntime(this.jobRuntime.copy())
            .swDataSets(List.copyOf(this.swDataSets))
            .status(this.status)
            .swmp(this.swmp.copy())
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Job job = (Job) o;
        return uuid.equals(job.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
