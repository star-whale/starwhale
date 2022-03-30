package ai.starwhale.mlops.domain.job;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface JobMapper {

    List<JobEntity> listJobs(@Param("projectId") Long projectId);

    JobEntity findJobById(@Param("jobId") Long jobId);

    int addJob(JobEntity jobEntity);
}
