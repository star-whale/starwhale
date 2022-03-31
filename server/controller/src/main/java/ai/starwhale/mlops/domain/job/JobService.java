/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */
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
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.schedule.TaskScheduler;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.scheduling.annotation.Scheduled;


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
  
    JobSpliterator jobSpliterator;

    TaskScheduler taskScheduler;

    LivingTaskStatusMachine livingTaskStatusMachine;


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
  
    /**
     * load created jobs from user at fixed delay
     */
    @Scheduled(fixedDelay = 3000)
    public void splitNewCreatedJobs(){
        final Stream<Job> allNewJobs = findAllNewJobs();
        allNewJobs.parallel().forEach(job->{
            //one transaction
            final List<TaskTrigger> taskTriggers = jobSpliterator.split(job);
            taskScheduler.adoptTasks(taskTriggers,job.getJobRuntime().getDeviceClass());
            livingTaskStatusMachine.adopt(taskTriggers.stream().map(TaskTrigger::getTask).collect(
                Collectors.toList()), TaskStatus.CREATED);
        });

    }

    Stream<Job> findAllNewJobs(){
        //TODO
        return null;
    }

    /**
     * transactional
     * jobStatus->TO_CANCEL; taskStatus->TO_CANCEL
     */
    public void cancelJob(Long jobId){
        //TODO
        livingTaskStatusMachine.adopt(null,TaskStatus.TO_CANCEL);
    }

}
