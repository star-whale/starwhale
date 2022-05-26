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
import ai.starwhale.mlops.domain.dag.DAGEditor;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.mapper.JobSWDSVersionMapper;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.task.TaskJobStatusHelper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.cache.LivingTaskCache;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("cacheWrapperForWatch")
    private LivingTaskCache livingTaskCache;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private ResultQuerier resultQuerier;

    @Resource
    private TaskJobStatusHelper taskJobStatusHelper;

    @Resource
    private StoragePathCoordinator storagePathCoordinator;

    @Resource
    private DAGEditor dagEditor;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private JobManager jobManager;

    public PageInfo<JobVO> listJobs(Long projectId, Long swmpId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<JobEntity> jobEntities = jobMapper.listJobs(projectId, swmpId);
        return PageUtil.toPageInfo(jobEntities, jobConvertor::convert);
    }

    public JobVO findJob(Long projectId, Long jobId) {
        JobEntity entity = jobMapper.findJobById(jobId);
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

    public Object getJobResult(Long projectId, Long jobId) {
        return resultQuerier.resultOfJob(jobId);
    }

    public Boolean updateJobComment(Long projectId, String jid, String comment) {
        int res;
        if(StrUtil.isNumeric(jid)){
            res = jobMapper.updateJobComment(Long.valueOf(jid), comment);
        } else {
            res = jobMapper.updateJobCommentByUUID(jid, comment);
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
        dagEditor.jobStatusChange(jobBoConverter.fromEntity(jobMapper.findJobById(jobEntity.getId())),JobStatus.CREATED);
        return jobEntity.getId();
    }

    /**
     * load created jobs from user at fixed delay
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 3000)
    public void splitNewCreatedJobs(){
        final Stream<Job> allNewJobs = findAllNewJobs();
        allNewJobs.parallel().forEach(job->{
            //one transaction
            final List<Task> tasks = jobSpliterator.split(job);
            livingTaskCache.adopt(tasks, TaskStatus.CREATED);
            List<Long> taskIds = tasks.parallelStream().map(Task::getId)
                .collect(Collectors.toList());
            tasks.parallelStream().forEach(task->dagEditor.taskStatusChange(task,TaskStatus.CREATED));
            swTaskScheduler.adoptTasks(livingTaskCache.ofIds(taskIds),job.getJobRuntime().getDeviceClass());
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
    public void cancelJob(Long jobId){
        Collection<Task> tasks = livingTaskCache.ofJob(jobId);
        if(null == tasks || tasks.isEmpty()){
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB).tip("freeze job can't be canceled "),
                HttpStatus.BAD_REQUEST);
        }
        JobStatus desiredJobStatus = taskJobStatusHelper.desiredJobStatus(tasks);
        if(desiredJobStatus != JobStatus.RUNNING
            && desiredJobStatus != JobStatus.PAUSED
            && desiredJobStatus != JobStatus.TO_COLLECT_RESULT
            && desiredJobStatus != JobStatus.COLLECTING_RESULT){
            throw new SWValidationException(ValidSubject.JOB).tip("not RUNNING/PAUSED/TO_COLLECT_RESULT/COLLECTING_RESULT job can't be canceled ");
        }
//        jobMapper.updateJobStatus(List.of(jobId), JobStatus.CANCELING);
        Optional<Task> anyTask = tasks.stream().findAny();
        synchronized (anyTask.get().getJob()){
            tasks.stream().filter(task ->
                    task.getStatus() == TaskStatus.RUNNING
                        || task.getStatus() == TaskStatus.PREPARING
                        || task.getStatus() == TaskStatus.ASSIGNING)
                .forEach(task -> task.setStatus(TaskStatus.TO_CANCEL));
            List<Task> directlyCanceledTasks = tasks.parallelStream()
                .filter(task -> task.getStatus() == TaskStatus.CREATED
                    || task.getStatus() == TaskStatus.PAUSED
                    || task.getStatus() == TaskStatus.UNKNOWN).collect(Collectors.toList());
            updateTaskStatus(directlyCanceledTasks, TaskStatus.CANCELED);
            swTaskScheduler.stopSchedule(directlyCanceledTasks.parallelStream().filter(task -> task.getStatus() == TaskStatus.CREATED).map(Task::getId).collect(
                Collectors.toList()));
        }
    }

    /**
     * transactional
     * jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void pauseJob(Long jobId){
        Collection<Task> tasks = livingTaskCache.ofJob(jobId);
        if(null == tasks || tasks.isEmpty()){
            throw new SWValidationException(ValidSubject.JOB).tip("freezing job can't be paused ");
        }
        List<Task> createdTasks = tasks.parallelStream()
            .filter(task -> task.getStatus().equals(TaskStatus.CREATED)).collect(
                Collectors.toList());
        if(null == createdTasks || createdTasks.isEmpty()){
            throw new SWValidationException(ValidSubject.JOB).tip("all tasks are assigned to agent, this job can't be paused now");
        }
//        jobMapper.updateJobStatus(List.of(jobId),JobStatus.PAUSED);\
        Optional<Task> anyTask = tasks.stream().findAny();
        synchronized (anyTask.get().getJob()){
            updateTaskStatus(createdTasks,TaskStatus.PAUSED);
            createdTasks.forEach(task -> task.setStatus(TaskStatus.PAUSED));
            swTaskScheduler.stopSchedule(createdTasks.parallelStream().map(Task::getId).collect(
                Collectors.toList()));
        }


    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;
    private void updateTaskStatus(Collection<Task> tasks,TaskStatus taskStatus) {
        if(CollectionUtils.isEmpty(tasks)){
            return;
        }
        //update in mem
        tasks.parallelStream().forEach(t->livingTaskCache.update(t.getId(),taskStatus));
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
    public void resumeJob(Long jobId){
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(jobEntity.getJobStatus() != JobStatus.PAUSED){
            throw new SWValidationException(ValidSubject.JOB).tip("unpaused job can't be resumed ");
        }
        Collection<Task> tasks = livingTaskCache.ofJob(jobId);
        if(null == tasks || tasks.isEmpty()){
            log.warn("no tasks found for job {} in task machine",jobId);
            return;
        }

        List<Task> pausedTasks = tasks.parallelStream()
            .filter(task -> task.getStatus().equals(TaskStatus.PAUSED))
            .collect(
                Collectors.toList());
        if(null == pausedTasks || pausedTasks.isEmpty()){
            return;
        }
        Optional<Task> anyTask = tasks.stream().findAny();
        Job job = anyTask.get().getJob();
        synchronized (job){
            updateTaskStatus(pausedTasks,TaskStatus.CREATED);
            pausedTasks.forEach(task -> task.setStatus(TaskStatus.CREATED));
            swTaskScheduler.adoptTasks(pausedTasks,job.getJobRuntime().getDeviceClass());
        }

    }

}
