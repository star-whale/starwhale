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

package ai.starwhale.mlops.domain.job.bo;

import ai.starwhale.mlops.common.TimeConcern;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
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
public class Job extends TimeConcern {

    Long id;

    String uuid;

    Project project;

    String evalJobDDL;

    Step currentStep;

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

    JobType type;

    /**
     * job result holding dir
     * Deprecated reason: use storage unaware of resultDir
     */
    @Deprecated
    String resultDir;

    List<Step> steps;

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

    @Override
    public String toString() {
        return "Job{" +
            "id=" + id +
            ", uuid='" + uuid + '\'' +
            '}';
    }
}
