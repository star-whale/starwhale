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
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.project.Project;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.task.StepHelper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
    private TaskMapper taskMapper;

    @Resource
    private ResultQuerier resultQuerier;

    @Resource
    private StoragePathCoordinator storagePathCoordinator;

    @Resource
    private ProjectManager projectManager;
    @Resource
    private JobManager jobManager;

    public PageInfo<JobVO> listJobs(String projectUrl, Long swmpId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(projectUrl);
        List<JobEntity> jobEntities = jobMapper.listJobs(projectId, swmpId);
        return PageUtil.toPageInfo(jobEntities, jobConvertor::convert);
    }

    public JobVO findJob(String projectUrl, String jobUrl) {
        Job job = jobManager.fromUrl(jobUrl);
        Project project = projectManager.fromUrl(projectUrl);
        JobEntity entity = jobManager.findJob(project, job);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip(String.format("Unable to find job %s", jobUrl)), HttpStatus.BAD_REQUEST);
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

    public Long createJob(Long projectId, Long imageId, Long modelVersionId, List<Long> datasetVersionIds, Integer deviceType, int deviceCount) {
        User user = userService.currentUserDetail();
        String jobUuid = IdUtil.simpleUUID();
        JobEntity jobEntity = JobEntity.builder()
            .ownerId(user.getId())
            .jobUuid(jobUuid)
            .createdTime(LocalDateTime.now())
            //.finishedTime(LocalDateTime.now())
            .durationMs(0L)
            .baseImageId(imageId)
            .projectId(projectId)
            .swmpVersionId(modelVersionId)
            .deviceType(deviceType)
            .deviceAmount(deviceCount)
            .resultOutputPath(storagePathCoordinator.generateResultMetricsPath(jobUuid))
            .jobStatus(JobStatus.CREATED)
            .type(JobType.EVALUATION)
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
            hotJobHolder.adopt(job);
            List<Task> readyToScheduleTasks = job.getSteps().parallelStream().map(Step::getTasks)
                .flatMap(Collection::stream)
                .filter(t -> t.getStatus() == TaskStatus.READY)
                .collect(Collectors.toList());
            swTaskScheduler.adoptTasks(readyToScheduleTasks,job.getJobRuntime().getDeviceClass());
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
        updateJobStatus(job, JobStatus.TO_CANCEL);

        tasksOfJob(job).filter(task ->
                task.getStatus() == TaskStatus.RUNNING
                    || task.getStatus() == TaskStatus.PREPARING
                    || task.getStatus() == TaskStatus.ASSIGNING)
            .forEach(task -> task.updateStatus(TaskStatus.TO_CANCEL));
        TaskStatusChangeWatcher.APPLIED_WATCHERS.set(Set.of(2,3,4));
        List<Task> directlyCanceledTasks = tasksOfJob(job)
            .filter(task -> task.getStatus() == TaskStatus.CREATED
                || task.getStatus() == TaskStatus.READY
                || task.getStatus() == TaskStatus.PAUSED
                || task.getStatus() == TaskStatus.UNKNOWN)
            .collect(Collectors.toList());
        directlyCanceledTasks.forEach(task -> task.updateStatus(TaskStatus.CANCELED));
        persistTaskStatus(directlyCanceledTasks,TaskStatus.CANCELED);
        TaskStatusChangeWatcher.APPLIED_WATCHERS.remove();
    }

    private void updateJobStatus(Job job, JobStatus jobStatus) {
        job.setStatus(jobStatus);
        jobMapper.updateJobStatus(List.of(job.getId()), jobStatus);
    }

    private Stream<Task> tasksOfJob(Job job) {
        return job.getSteps().stream()
            .map(step -> step.getTasks())
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
            throw new SWValidationException(ValidSubject.JOB).tip("freezing job can't be paused ");
        }
        Job job = jobs.stream().findAny().get();
        List<Task> readyToScheduleTasks = tasksOfJob(job)
            .filter(task -> task.getStatus() == TaskStatus.READY ).collect(
                Collectors.toList());
        if(null == readyToScheduleTasks || readyToScheduleTasks.isEmpty()){
            throw new SWValidationException(ValidSubject.JOB).tip("all tasks are assigned to agent, this job can't be paused now");
        }
        updateJobStatus(job, JobStatus.PAUSED);
        persistTaskStatus(readyToScheduleTasks,TaskStatus.PAUSED);
        readyToScheduleTasks.forEach(task -> task.updateStatus(TaskStatus.PAUSED));

    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;
    private void persistTaskStatus(Collection<Task> tasks,TaskStatus taskStatus) {
        if(CollectionUtils.isEmpty(tasks)){
            return;
        }
        //save to db
        List<Long> taskIds = tasks.parallelStream().map(Task::getId).collect(
            Collectors.toList());
        BatchOperateHelper.doBatch(taskIds
            , taskStatus
            , (tsks, status) -> taskMapper.updateTaskStatus(
                tsks.stream().collect(Collectors.toList()),
                status)
            , MAX_BATCH_SIZE);
    }

    /**
     * transactional
     * jobStatus PAUSED->RUNNING; taskStatus PAUSED->CREATED
     */
    @Transactional
    public void resumeJob(String jobUrl){
        Long jobId = jobManager.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if(null == jobs || jobs.isEmpty()){
            log.warn("no tasks found for job {} in task machine",jobId);
            return;
        }
        Job job = jobs.stream().findAny().get();
        if(job.getStatus() != JobStatus.PAUSED){
            throw new SWValidationException(ValidSubject.JOB).tip("unpaused job can't be resumed ");
        }

        updateJobStatus(job, JobStatus.RUNNING);
        List<Task> pausedTasks = tasksOfJob(job)
            .filter(task -> task.getStatus().equals(TaskStatus.PAUSED))
            .collect(
                Collectors.toList());
        if(null == pausedTasks || pausedTasks.isEmpty()){
            return;
        }
        persistTaskStatus(pausedTasks,TaskStatus.READY);
        pausedTasks.forEach(task -> task.updateStatus(TaskStatus.READY));

    }

}
