/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */
package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskMapper;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
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
import org.springframework.transaction.annotation.Transactional;


@Service
public class JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private JobConvertor jobConvertor;

    JobBoConverter jobBoConverter;

    @Resource
    private UserService userService;

    JobSpliterator jobSpliterator;

    TaskScheduler taskScheduler;

    LivingTaskStatusMachine livingTaskStatusMachine;

    TaskMapper taskMapper;

    TaskBoConverter taskBoConverter;


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
            .jobStatus(JobStatus.CREATED.getValue())
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
        final List<JobEntity> newJobs = jobMapper.findJobByStatus(JobStatus.CREATED.getValue());
        return newJobs.stream().map(entity-> jobBoConverter.fromEntity(entity));
    }

    /**
     * transactional
     * jobStatus->TO_CANCEL; taskStatus->TO_CANCEL
     */
    @Transactional
    public void cancelJob(Long jobId){
        jobMapper.updateJobStatus(List.of(jobId), JobStatus.CANCELED.getValue());
        final List<TaskEntity> taskEntities = taskMapper.listTasks(jobId);
        taskMapper.updateTaskStatus(taskEntities.parallelStream().map(TaskEntity::getId).collect(
            Collectors.toList()), TaskStatus.TO_CANCEL.getOrder());
        livingTaskStatusMachine.adopt(taskEntities.parallelStream()
                .map(entity -> taskBoConverter.fromTaskEntity(entity)).collect(Collectors.toList()),
            TaskStatus.TO_CANCEL);
    }

}
