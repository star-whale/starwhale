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

package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobMapper {

    List<JobEntity> listJobs(@Param("projectId") Long projectId, @Param("modelId") Long modelId);

    /**
     * built-in jobs are excluded
     *
     * @param projectId the project where the job belongs to
     * @param modelId   the model where the job run against
     * @return all the jobs matched
     */
    List<JobEntity> listUserJobs(@Param("projectId") Long projectId, @Param("modelId") Long modelId);

    JobEntity findJobById(@Param("jobId") Long jobId);

    JobEntity findJobByUuid(@Param("uuid") String uuid);

    int addJob(@Param("job") JobEntity jobEntity);

    List<JobEntity> findJobByStatusIn(@Param("jobStatuses") List<JobStatus> jobStatuses);

    void updateJobStatus(@Param("jobIds") List<Long> jobIds, @Param("jobStatus") JobStatus jobStatus);

    void updateJobFinishedTime(
            @Param("jobIds") List<Long> jobIds,
            @Param("finishedTime") Date finishedTime,
            @Param("duration") Long duration);

    int updateJobComment(@Param("id") Long id, @Param("comment") String comment);

    int updateJobCommentByUuid(@Param("uuid") String uuid, @Param("comment") String comment);

    int removeJob(@Param("id") Long id);

    int removeJobByUuid(@Param("uuid") String uuid);

    int recoverJob(@Param("id") Long id);

    int recoverJobByUuid(@Param("uuid") String uuid);

    int updateJobPinStatus(@Param("id") Long id, @Param("pinnedTime") Date pinnedTime);

    int updateJobPinStatusByUuid(
            @Param("uuid") String uuid,
            @Param("pinnedTime") Date pinnedTime);
}
