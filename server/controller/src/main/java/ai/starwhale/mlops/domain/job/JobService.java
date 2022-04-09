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
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.swds.SWDSConvertor;
import ai.starwhale.mlops.domain.swds.SWDSVersionConvertor;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskMapper;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.resulting.ResultCollectManager;
import ai.starwhale.mlops.schedule.TaskScheduler;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private JobSWDSVersionMapper jobSWDSVersionMapper;

    @Resource
    private SWDSVersionConvertor swdsVersionConvertor;

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
    private TaskScheduler taskScheduler;

    @Resource
    private LivingTaskStatusMachine livingTaskStatusMachine;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskBoConverter taskBoConverter;

    @Resource
    private ResultCollectManager resultCollectManager;


    public List<JobVO> listJobs(String projectId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<JobEntity> jobEntities = jobMapper.listJobs(idConvertor.revert(projectId));

        return jobEntities.stream()
            .map(jobConvertor::convert)
            .collect(Collectors.toList());
    }

    public JobVO findJob(String projectId, String jobId) {
        JobEntity entity = jobMapper.findJobById(idConvertor.revert(jobId));

        JobVO jobVO = jobConvertor.convert(entity);
        List<SWDatasetVersionEntity> dsvEntities = jobSWDSVersionMapper.listSWDSVersionsByJobId(
            entity.getId());

        List<DatasetVersionVO> dsvList = dsvEntities.stream()
            .map(swdsVersionConvertor::convert)
            .collect(Collectors.toList());

        jobVO.setDatasets(dsvList);

        EvaluationResult evaluationResult = resultCollectManager.resultOfJob(entity.getId());
//        EvaluationResult evaluationResult = new EvaluationResult("MCResultCollector", List.of());
        jobVO.setEvaluationResult(evaluationResult);

        return jobVO;
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
            taskScheduler.adoptTasks(tasks,job.getJobRuntime().getDeviceClass());
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
        jobMapper.updateJobStatus(List.of(jobId), JobStatus.TO_CANCEL.getValue());
        final List<TaskEntity> taskEntities = taskMapper.listTasks(jobId);
        taskMapper.updateTaskStatus(taskEntities.parallelStream().map(TaskEntity::getId).collect(
            Collectors.toList()), TaskStatus.CANCEL.getOrder());
        final JobEntity job = jobMapper.findJobById(jobId);
        livingTaskStatusMachine.update(taskBoConverter.fromTaskEntity(taskEntities,jobBoConverter.fromEntity(job)),
            new StagingTaskStatus(TaskStatus.CANCEL));
    }

}
