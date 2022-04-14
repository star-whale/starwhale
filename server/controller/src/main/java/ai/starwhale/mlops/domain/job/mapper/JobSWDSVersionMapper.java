package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface JobSWDSVersionMapper {

    List<SWDatasetVersionEntity> listSWDSVersionsByJobId(@Param("jobId") Long jobId);

    int addJobSWDSVersions(@Param("jobId") Long jodId, @Param("dsvIds") List<Long> dsvIds);

}
