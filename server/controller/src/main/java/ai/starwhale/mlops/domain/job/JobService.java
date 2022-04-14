/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */
package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.api.protocol.resulting.EvaluationResult;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.TaskJobStatusHelper;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import ai.starwhale.mlops.resulting.ResultCollectManager;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private JobSWDSVersionMapper jobSWDSVersionMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private JobConvertor jobConvertor;

    @Resource
    private JobBoConverter jobBoConverter;

    @Resource
    private UserService userService;

    @Resource
    private JobSpliterator jobSpliterator;

    @Resource
    private SWTaskScheduler SWTaskScheduler;

    @Resource
    private LivingTaskStatusMachine livingTaskStatusMachine;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskBoConverter taskBoConverter;

    @Resource
    private ResultCollectManager resultCollectManager;

    @Resource
    private TaskJobStatusHelper taskJobStatusHelper;

    public List<JobVO> listJobs(String projectId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<JobEntity> jobEntities = jobMapper.listJobs(idConvertor.revert(projectId));

        return jobEntities.stream()
            .map(jobConvertor::convert)
            .collect(Collectors.toList());
    }

    public JobVO findJob(String projectId, String jobId) {
        JobEntity entity = jobMapper.findJobById(idConvertor.revert(jobId));
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip(String.format("Unable to find job %s", jobId)), HttpStatus.BAD_REQUEST);
        }
        JobVO jobVO = jobConvertor.convert(entity);
        List<SWDatasetVersionEntity> dsvEntities = jobSWDSVersionMapper.listSWDSVersionsByJobId(
            entity.getId());

        List<String> idList = dsvEntities.stream()
            .map(SWDatasetVersionEntity::getVersionName)
            .collect(Collectors.toList());

        jobVO.setDatasets(idList);

        return jobVO;
    }

    public EvaluationResult getJobResult(String projectId, String jobId) {
        return resultCollectManager.resultOfJob(
            idConvertor.revert(jobId));
    }

    public String createJob(JobRequest jobRequest, String projectId) {
        User user = userService.currentUserDetail();
        JobEntity jobEntity = JobEntity.builder()
            .ownerId(idConvertor.revert(user.getId()))
            .jobUuid(IdUtil.simpleUUID())
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

        String datasetVersionIds = jobRequest.getDatasetVersionIds();
        if(datasetVersionIds == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip("Dataset Version ids must be set."), HttpStatus.BAD_REQUEST);
        }
        List<Long> dsvIds = Arrays.stream(datasetVersionIds.split("[,;]"))
            .map(idConvertor::revert)
            .collect(Collectors.toList());

        jobSWDSVersionMapper.addJobSWDSVersions(jobEntity.getId(), dsvIds);

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
            final List<Task> tasks = jobSpliterator.split(job);
            SWTaskScheduler.adoptTasks(tasks,job.getJobRuntime().getDeviceClass());
            livingTaskStatusMachine.adopt(tasks, new StagingTaskStatus(TaskStatus.CREATED));
        });

    }

    Stream<Job> findAllNewJobs(){
        final List<JobEntity> newJobs = jobMapper.findJobByStatusIn(List.of(JobStatus.CREATED.getValue()));
        return newJobs.stream().map(entity-> jobBoConverter.fromEntity(entity));
    }

    /**
     * transactional
     * jobStatus->TO_CANCEL; taskStatus->TO_CANCEL
     */
    @Transactional
    public void cancelJob(Long jobId){
        Collection<Task> tasks = livingTaskStatusMachine.ofJob(jobId);
        if(null == tasks || tasks.isEmpty()){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB).tip("freezing job can't be canceled "),
                HttpStatus.BAD_REQUEST);
        }
        JobStatus desiredJobStatus = taskJobStatusHelper.desiredJobStatus(tasks);
        if(desiredJobStatus != JobStatus.RUNNING){
            throw new SWValidationException(ValidSubject.JOB).tip("not running job can't be canceled ");
        }
        jobMapper.updateJobStatus(List.of(jobId), JobStatus.TO_CANCEL.getValue());
        taskMapper.updateTaskStatus(tasks.parallelStream().map(Task::getId).collect(
            Collectors.toList()), new StagingTaskStatus(TaskStatus.CANCEL).getValue());
        livingTaskStatusMachine.update(tasks,
            new StagingTaskStatus(TaskStatus.CANCEL));
    }

    /**
     * transactional
     * jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void pauseJob(Long jobId){
        Collection<Task> tasks = livingTaskStatusMachine.ofJob(jobId);
        if(null == tasks || tasks.isEmpty()){
            throw new SWValidationException(ValidSubject.JOB).tip("freezing job can't be paused ");
        }
        List<Task> createdTasks = tasks.parallelStream()
            .filter(task -> task.getStatus() == new StagingTaskStatus(TaskStatus.CREATED)).collect(
                Collectors.toList());
        if(null == createdTasks || createdTasks.isEmpty()){
            throw new SWValidationException(ValidSubject.JOB).tip("all tasks are assigned to agent, this job can't be paused now");
        }
        jobMapper.updateJobStatus(List.of(jobId), JobStatus.PAUSED.getValue());
        taskMapper.updateTaskStatus(createdTasks.parallelStream().map(Task::getId).collect(
            Collectors.toList()), new StagingTaskStatus(TaskStatus.PAUSED).getValue());
        livingTaskStatusMachine.update(createdTasks,
            new StagingTaskStatus(TaskStatus.PAUSED));
    }

    /**
     * transactional
     * jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void resumeJob(Long jobId){
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(JobStatus.from(jobEntity.getJobStatus()) != JobStatus.PAUSED){
            throw new SWValidationException(ValidSubject.JOB).tip("unpaused job can't be resumed ");
        }
        jobMapper.updateJobStatus(List.of(jobId), JobStatus.RUNNING.getValue());
        Collection<Task> tasks = livingTaskStatusMachine.ofJob(jobId);
        if(null == tasks || tasks.isEmpty()){
            log.warn("no tasks found for job {} in task machine",jobId);
            return;
        }
        List<Task> pausedTasks = tasks.parallelStream()
            .filter(task -> task.getStatus() == new StagingTaskStatus(TaskStatus.PAUSED)).collect(
                Collectors.toList());
        if(null == pausedTasks || pausedTasks.isEmpty()){
            return;
        }
        taskMapper.updateTaskStatus(pausedTasks.parallelStream().map(Task::getId).collect(
            Collectors.toList()), new StagingTaskStatus(TaskStatus.CREATED).getValue());
        livingTaskStatusMachine.update(pausedTasks,
            new StagingTaskStatus(TaskStatus.CREATED));
    }

}
