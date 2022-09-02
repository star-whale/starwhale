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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.converter.JobConvertor;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.RuntimeManager;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.SwdsManager;
import ai.starwhale.mlops.domain.swmp.SwmpManager;
import ai.starwhale.mlops.domain.system.resourcepool.ResourcePoolManager;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import ai.starwhale.mlops.resulting.ResultQuerier;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private JobSWDSVersionMapper jobSWDSVersionMapper;

    @Resource
    private JobConvertor jobConvertor;

    @Resource
    private JobBoConverter jobBoConverter;

    @Resource
    private UserService userService;

    @Resource
    private JobSpliterator jobSpliterator;

    @Resource
    private SWTaskScheduler swTaskScheduler;

    @Resource
    private HotJobHolder hotJobHolder;

    @Resource
    private JobLoader jobLoader;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private ResultQuerier resultQuerier;

    @Resource
    private StoragePathCoordinator storagePathCoordinator;

    @Resource
    private ProjectManager projectManager;
    @Resource
    private JobManager jobManager;

    @Resource
    private SwmpManager swmpManager;

    @Resource
    private SwdsManager swdsManager;

    @Resource
    private RuntimeManager runtimeManager;

    @Resource
    private ResourcePoolManager resourcePoolManager;

    public PageInfo<JobVO> listJobs(String projectUrl, Long swmpId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(projectUrl);
        List<JobEntity> jobEntities = jobMapper.listJobs(projectId, swmpId);
        return PageUtil.toPageInfo(jobEntities, jobConvertor::convert);
    }

    public JobVO findJob(String projectUrl, String jobUrl) {
        Job job = jobManager.fromUrl(jobUrl);
        JobEntity entity = jobManager.findJob(job);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip(String.format("Unable to find job %s", jobUrl)), HttpStatus.BAD_REQUEST);
        }

        return jobConvertor.convert(entity);
    }

    public Object getJobResult(String projectUrl, String jobUrl) {
        Long jobId = jobManager.getJobId(jobUrl);
        return resultQuerier.resultOfJob(jobId);
    }

    public Boolean updateJobComment(String projectUrl, String jobUrl, String comment) {
        Job job = jobManager.fromUrl(jobUrl);
        int res;
        if(job.getId() != null){
            res = jobMapper.updateJobComment(job.getId(), comment);
        } else {
            res = jobMapper.updateJobCommentByUUID(job.getUuid(), comment);
        }
        return res > 0;
    }

    public Boolean removeJob(String projectUrl, String jobUrl) {
        Job job = jobManager.fromUrl(jobUrl);
        int res = 0;
        if(job.getId() != null) {
            res = jobMapper.removeJob(job.getId());
        } else if (!StrUtil.isEmpty(job.getUuid())) {
            res = jobMapper.removeJobByUUID(job.getUuid());
        }

        return res > 0;
    }

    public Boolean recoverJob(String projectUrl, String jobUrl) {
        Job job = jobManager.fromUrl(jobUrl);
        int res = 0;
        if(job.getId() != null) {
            res = jobMapper.recoverJob(job.getId());
        } else if (!StrUtil.isEmpty(job.getUuid())) {
            res = jobMapper.recoverJobByUUID(job.getUuid());
        }

        return res > 0;
    }

    private static Device.Clazz getDeviceClazz(String device) {
        try {
            if(StrUtil.isNumeric(device)) {
                return Device.Clazz.from(Integer.parseInt(device));

            } else {
                return Enum.valueOf(Device.Clazz.class, device);
            }
        } catch (IllegalArgumentException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip(String.format("Unable to find device %s", device)), HttpStatus.BAD_REQUEST);
        }
    }
    public Long createJob(String projectUrl,
        String modelVersionUrl, String datasetVersionUrls, String runtimeVersionUrl,
        String deviceType, Float deviceCount, String comment, String resourcePool) {
        User user = userService.currentUserDetail();
        String jobUuid = IdUtil.simpleUUID();
        Long projectId = projectManager.getProjectId(projectUrl);
        Long runtimeVersionId = runtimeManager.getRuntimeVersionId(runtimeVersionUrl, null);
        Long modelVersionId = swmpManager.getSWMPVersionId(modelVersionUrl, null);
        Integer deviceValue = getDeviceClazz(deviceType).getValue();
        Long resourcePoolId = resourcePoolManager.getResourcePoolId(resourcePool);
        JobEntity jobEntity = JobEntity.builder()
            .ownerId(user.getId())
            .jobUuid(jobUuid)
            .createdTime(LocalDateTime.now())
            //.finishedTime(LocalDateTime.now())
            .durationMs(0L)
            .runtimeVersionId(runtimeVersionId)
            .projectId(projectId)
            .swmpVersionId(modelVersionId)
            .deviceType(deviceValue)
            .deviceAmount(Float.valueOf(deviceCount*1000).intValue())
            .comment(comment)
            .resultOutputPath(storagePathCoordinator.generateResultMetricsPath(jobUuid))
            .jobStatus(JobStatus.CREATED)
            .type(JobType.EVALUATION)
            .resourcePoolId(resourcePoolId)
            .build();

        jobMapper.addJob(jobEntity);
        log.info("Job has been created. ID={}, UUID={}", jobEntity.getId(), jobEntity.getJobUuid());

//        String datasetVersionIds = jobRequest.getDatasetVersionIds();
//        if(datasetVersionIds == null) {
//            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
//                .tip("Dataset Version ids must be set."), HttpStatus.BAD_REQUEST);
//        }
//        List<Long> dsvIds = Arrays.stream(datasetVersionIds.split("[,;]"))
//            .map(idConvertor::revert)
//            .collect(Collectors.toList());

        List<Long> datasetVersionIds = Arrays.stream(datasetVersionUrls.split("[,;]"))
            .map(url -> swdsManager.getSWDSVersionId(url, null))
            .collect(Collectors.toList());
        jobSWDSVersionMapper.addJobSWDSVersions(jobEntity.getId(), datasetVersionIds);
        return jobEntity.getId();
    }

    /**
     * load created jobs from user at fixed delay
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void splitNewCreatedJobs(){
        final Stream<Job> allNewJobs = findAllNewJobs();
        allNewJobs.parallel().forEach(job->{
            //one transaction
            jobSpliterator.split(job);
            jobLoader.loadEntities(List.of(jobMapper.findJobById(job.getId())),false,true);
            /*hotJobHolder.adopt(job);
            List<Task> readyToScheduleTasks = job.getSteps().parallelStream().map(Step::getTasks)
                .flatMap(Collection::stream)
                .filter(t -> t.getStatus() == TaskStatus.READY)
                .collect(Collectors.toList());
            swTaskScheduler.adopt(readyToScheduleTasks,job.getJobRuntime().getDeviceClass());*/
        });

    }

    Stream<Job> findAllNewJobs(){
        final List<JobEntity> newJobs = jobMapper.findJobByStatusIn(List.of(JobStatus.CREATED));
        return newJobs.stream().map(entity-> jobBoConverter.fromEntity(entity));
    }

    /**
     * transactional
     * jobStatus->TO_CANCEL; RUNNING/PREPARING/ASSIGNING->TO_CANCEL;CREATED/PAUSED/UNKNOWN->CANCELED
     */
    @Transactional
    public void cancelJob(String jobUrl){
        Long jobId = jobManager.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if(null == jobs || jobs.isEmpty()){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB).tip("freeze job can't be canceled "),
                HttpStatus.BAD_REQUEST);
        }
        Job job = jobs.stream().findAny().get();
        if(job.getStatus() != JobStatus.RUNNING
            && job.getStatus() != JobStatus.PAUSED){
            throw new SWValidationException(ValidSubject.JOB).tip("not RUNNING/PAUSED job can't be canceled ");
        }

        List<Task> directlyCanceledTasks = tasksOfJob(job)
            .filter(task -> task.getStatus() != TaskStatus.SUCCESS
                && task.getStatus() != TaskStatus.FAIL
                && task.getStatus() != TaskStatus.CREATED)
            .collect(Collectors.toList());
        batchPersistTaskStatus(directlyCanceledTasks,TaskStatus.CANCELED);
        updateWithoutPersistWatcher(directlyCanceledTasks, TaskStatus.CANCELED);
    }

    private Stream<Task> tasksOfJob(Job job) {
        return job.getSteps().stream()
            .map(Step::getTasks)
            .flatMap(Collection::stream);
    }

    /**
     * transactional
     * jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void pauseJob(String jobUrl){
        Long jobId = jobManager.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if(null == jobs || jobs.isEmpty()){
            throw new SWValidationException(ValidSubject.JOB).tip("frozen job can't be paused ");
        }
        Job job = jobs.stream().findAny().get();
        List<Task> notRunningTasks = tasksOfJob(job)
            .filter(task -> task.getStatus() != TaskStatus.SUCCESS
                && task.getStatus() != TaskStatus.FAIL
                && task.getStatus() != TaskStatus.CREATED)
            .collect(Collectors.toList());
        if(null == notRunningTasks || notRunningTasks.isEmpty()){
            return;
        }
        batchPersistTaskStatus(notRunningTasks,TaskStatus.PAUSED);
        updateWithoutPersistWatcher(notRunningTasks, TaskStatus.PAUSED);

    }

    private void updateWithoutPersistWatcher(List<Task> tasks, TaskStatus taskStatus) {
        CompletableFuture.runAsync(()->{
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForPersist.class));
            tasks.forEach(task -> {
                task.updateStatus(taskStatus);
            });
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
        });
    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;
    private void batchPersistTaskStatus(Collection<Task> tasks,TaskStatus taskStatus) {
        if(CollectionUtils.isEmpty(tasks)){
            return;
        }
        //save to db
        List<Long> taskIds = tasks.parallelStream().map(Task::getId).collect(
            Collectors.toList());
        BatchOperateHelper.doBatch(taskIds
            , taskStatus
            , (tsks, status) -> taskMapper.updateTaskStatus(
                new ArrayList<>(tsks),
                status)
            , MAX_BATCH_SIZE);
    }

    /**
     *
     * jobStatus PAUSED->RUNNING; taskStatus PAUSED->CREATED
     * jobStatus FAILED->RUNNING; taskStatus PAUSED->CREATED
     */
    public void resumeJob(String jobUrl){
        Long jobId = jobManager.getJobId(jobUrl);
        JobEntity  jobEntity= jobMapper.findJobById(jobId);
        if(null == jobEntity ){
            throw new SWValidationException(ValidSubject.JOB).tip("job not exists");
        }
        if(jobEntity.getJobStatus() != JobStatus.PAUSED
            && jobEntity.getJobStatus() != JobStatus.FAIL
            && jobEntity.getJobStatus() != JobStatus.CANCELED){
            throw new SWValidationException(ValidSubject.JOB).tip("only failed/paused/canceled job can be resumed ");
        }
        jobLoader.loadEntities(List.of(jobEntity),true,true);
    }

}
