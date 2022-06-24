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
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface JobMapper {

    List<JobEntity> listJobs(@Param("projectId") Long projectId, @Param("swmpId") Long swmpId);

    List<JobEntity> listJobsByStatus(@Param("projectId") Long projectId, @Param("swmpId") Long swmpId, @Param("jobStatus") JobStatus jobStatus);

    JobEntity findJobById(@Param("jobId") Long jobId);

    JobEntity findJobByUUID(@Param("uuid") String uuid);

    int addJob(@Param("job")JobEntity jobEntity);

    List<JobEntity> findJobByStatusIn(@Param("jobStatuses") List<JobStatus> jobStatuses);

    void updateJobStatus(@Param("jobIds") List<Long> jobIds,@Param("jobStatus") JobStatus jobStatus);

    void updateJobFinishedTime(@Param("jobIds") List<Long> jobIds,@Param("finishedTime")LocalDateTime finishedTime);

    void updateJobResultPath(@Param("jobId")Long jobId, @Param("resultDir")String resultDir);

    int updateJobComment(@Param("id")Long id, @Param("comment")String comment);

    int updateJobCommentByUUID(@Param("uuid")String uuid, @Param("comment")String comment);

    int removeJob(@Param("id")Long id);

    int removeJobByUUID(@Param("uuid")String uuid);

    int recoverJob(@Param("id")Long id);

    int recoverJobByUUID(@Param("uuid")String uuid);
}
