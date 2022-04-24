package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface JobMapper {

    List<JobEntity> listJobs(@Param("projectId") Long projectId, @Param("swmpId") Long swmpId);

    JobEntity findJobById(@Param("jobId") Long jobId);

    int addJob(JobEntity jobEntity);

    List<JobEntity> findJobByStatusIn(@Param("jobStatuses") List<JobStatus> jobStatuses);

    void updateJobStatus(@Param("jobIds") List<Long> jobIds,@Param("jobStatus") JobStatus jobStatus);
}
