package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.task.TaskConvertor;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskMapper;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import com.github.pagehelper.PageHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private JobConvertor jobConvertor;

    @Resource
    private UserService userService;


    public List<JobVO> listJobs(String projectId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<JobEntity> jobEntities = jobMapper.listJobs(idConvertor.revert(projectId));

        return jobEntities.stream()
            .map(jobConvertor::convert)
            .collect(Collectors.toList());
    }

    public JobVO findJob(String projectId, String jobId) {
        JobEntity entity = jobMapper.findJobById(idConvertor.revert(jobId));
        return jobConvertor.convert(entity);
    }

    public String createJob(JobRequest jobRequest, String projectId) {
        User user = userService.currentUserDetail();
        JobEntity jobEntity = JobEntity.builder()
            .ownerId(idConvertor.revert(user.getId()))
            .jobUuid(RandomUtil.randomHexString(16))
            .createdTime(LocalDateTime.now())
            //.finishedTime(LocalDateTime.now())
            .durationMs(0L)
            .baseImageId(idConvertor.revert(jobRequest.getBaseImageId()))
            .projectId(idConvertor.revert(projectId))
            .swmpVersionId(idConvertor.revert(jobRequest.getModelVersionId()))
            .deviceType(Integer.valueOf(jobRequest.getDeviceId()))
            .deviceAmount(jobRequest.getDeviceCount())
            .resultOutputPath(jobRequest.getResultOutputPath())
            .jobStatus(1)
            .build();
        jobMapper.addJob(jobEntity);
        return idConvertor.convert(jobEntity.getId());
    }

}
