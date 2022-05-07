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
