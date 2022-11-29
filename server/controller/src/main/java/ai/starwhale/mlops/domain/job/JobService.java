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

import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.mapper.JobDatasetVersionMapper;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.resulting.ResultQuerier;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class JobService {

    private final JobMapper jobMapper;
    private final JobDatasetVersionMapper jobDatasetVersionMapper;
    private final TaskMapper taskMapper;
    private final JobConverter jobConvertor;
    private final JobBoConverter jobBoConverter;
    private final JobSpliterator jobSpliterator;
    private final HotJobHolder hotJobHolder;
    private final JobLoader jobLoader;
    private final ResultQuerier resultQuerier;
    private final StoragePathCoordinator storagePathCoordinator;
    private final UserService userService;
    private final ProjectManager projectManager;
    private final JobManager jobManager;
    private final ModelDao modelDao;
    private final DatasetDao datasetDao;
    private final RuntimeDao runtimeDao;
    private final JobUpdateHelper jobUpdateHelper;

    private final TrashService trashService;

    public JobService(JobBoConverter jobBoConverter, JobMapper jobMapper,
            JobDatasetVersionMapper jobDatasetVersionMapper,
            TaskMapper taskMapper, JobConverter jobConvertor, RuntimeDao runtimeDao,
            JobSpliterator jobSpliterator, HotJobHolder hotJobHolder,
            ProjectManager projectManager, JobManager jobManager, JobLoader jobLoader, ModelDao modelDao,
            ResultQuerier resultQuerier, DatasetDao datasetDao, StoragePathCoordinator storagePathCoordinator,
            UserService userService, JobUpdateHelper jobUpdateHelper, TrashService trashService) {
        this.jobBoConverter = jobBoConverter;
        this.jobMapper = jobMapper;
        this.jobDatasetVersionMapper = jobDatasetVersionMapper;
        this.taskMapper = taskMapper;
        this.jobConvertor = jobConvertor;
        this.runtimeDao = runtimeDao;
        this.jobSpliterator = jobSpliterator;
        this.hotJobHolder = hotJobHolder;
        this.projectManager = projectManager;
        this.jobManager = jobManager;
        this.jobLoader = jobLoader;
        this.modelDao = modelDao;
        this.resultQuerier = resultQuerier;
        this.datasetDao = datasetDao;
        this.storagePathCoordinator = storagePathCoordinator;
        this.userService = userService;
        this.jobUpdateHelper = jobUpdateHelper;
        this.trashService = trashService;
    }

    public PageInfo<JobVo> listJobs(String projectUrl, Long modelId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(projectUrl);
        List<JobEntity> jobEntities = jobMapper.listJobs(projectId, modelId);
        return PageUtil.toPageInfo(jobEntities, jobConvertor::convert);
    }

    public JobVo findJob(String projectUrl, String jobUrl) {
        Job job = jobManager.fromUrl(jobUrl);
        JobEntity entity = jobManager.findJob(job);
        if (entity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, String.format("Unable to find job %s", jobUrl)),
                    HttpStatus.BAD_REQUEST);
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
        if (job.getId() != null) {
            res = jobMapper.updateJobComment(job.getId(), comment);
        } else {
            res = jobMapper.updateJobCommentByUuid(job.getUuid(), comment);
        }
        return res > 0;
    }

    public Boolean removeJob(String projectUrl, String jobUrl) {
        Long jobId = jobManager.getJobId(jobUrl);
        Trash trash = Trash.builder()
                .projectId(projectManager.getProjectId(projectUrl))
                .objectId(jobId)
                .type(Type.EVALUATION)
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());

        int res = jobMapper.removeJob(jobId);
        return res > 0;
    }

    public Boolean recoverJob(String projectUrl, String jobUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    public Long createJob(String projectUrl,
            String modelVersionUrl, String datasetVersionUrls, String runtimeVersionUrl,
            String comment, String resourcePool,
            String stepSpecOverWrites) {
        User user = userService.currentUserDetail();
        String jobUuid = IdUtil.simpleUUID();
        Long projectId = projectManager.getProjectId(projectUrl);
        Long runtimeVersionId = runtimeDao.getRuntimeVersionId(runtimeVersionUrl, null);
        Long modelVersionId = modelDao.getModelVersionId(modelVersionUrl, null);
        JobEntity jobEntity = JobEntity.builder()
                .ownerId(user.getId())
                .jobUuid(jobUuid)
                .runtimeVersionId(runtimeVersionId)
                .projectId(projectId)
                .modelVersionId(modelVersionId)
                .comment(comment)
                .resultOutputPath(storagePathCoordinator.allocateResultMetricsPath(jobUuid))
                .jobStatus(JobStatus.CREATED)
                .type(JobType.EVALUATION)
                .resourcePool(resourcePool)
                .stepSpec(stepSpecOverWrites)
                .build();

        jobMapper.addJob(jobEntity);
        log.info("Job has been created. ID={}, UUID={}", jobEntity.getId(), jobEntity.getJobUuid());

        List<Long> datasetVersionIds = Arrays.stream(datasetVersionUrls.split("[,;]"))
                .map(url -> datasetDao.getDatasetVersionId(url, null))
                .collect(Collectors.toList());
        jobDatasetVersionMapper.addJobDatasetVersions(jobEntity.getId(), datasetVersionIds);
        return jobEntity.getId();
    }

    /**
     * load created jobs from user at fixed delay
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void splitNewCreatedJobs() {
        jobMapper.findJobByStatusIn(List.of(JobStatus.CREATED))
                .forEach(jobEntity -> {
                    //one transaction
                    try {
                        jobSpliterator.split(jobEntity);
                        Job job = jobBoConverter.fromEntity(jobEntity);
                        jobLoader.load(job, false);
                        jobUpdateHelper.updateJob(job);
                    } catch (Exception e) {
                        log.error("splitting job error {}", jobEntity.getId(), e);
                        jobMapper.updateJobStatus(List.of(jobEntity.getId()), JobStatus.FAIL);
                    }

                });

    }

    /**
     * transactional jobStatus->TO_CANCEL; RUNNING/PREPARING/ASSIGNING->TO_CANCEL;CREATED/PAUSED/UNKNOWN->CANCELED
     */
    @Transactional
    public void cancelJob(String jobUrl) {
        Long jobId = jobManager.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if (null == jobs || jobs.isEmpty()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "freeze job can't be canceled "),
                    HttpStatus.BAD_REQUEST);
        }
        Job job = jobs.stream().findAny().get();
        if (job.getStatus() != JobStatus.RUNNING
                && job.getStatus() != JobStatus.PAUSED) {
            throw new SwValidationException(ValidSubject.JOB, "not RUNNING/PAUSED job can't be canceled ");
        }

        List<Task> directlyCanceledTasks = tasksOfJob(job)
                .filter(task -> task.getStatus() != TaskStatus.SUCCESS
                        && task.getStatus() != TaskStatus.FAIL
                        && task.getStatus() != TaskStatus.CREATED)
                .collect(Collectors.toList());
        batchPersistTaskStatus(directlyCanceledTasks, TaskStatus.CANCELED);
        updateWithoutPersistWatcher(directlyCanceledTasks, TaskStatus.CANCELED);
    }

    private Stream<Task> tasksOfJob(Job job) {
        return job.getSteps().stream()
                .map(Step::getTasks)
                .flatMap(Collection::stream);
    }

    /**
     * transactional jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void pauseJob(String jobUrl) {
        Long jobId = jobManager.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if (null == jobs || jobs.isEmpty()) {
            throw new SwValidationException(ValidSubject.JOB, "frozen job can't be paused ");
        }
        Job job = jobs.stream().findAny().get();
        List<Task> notRunningTasks = tasksOfJob(job)
                .filter(task -> task.getStatus() != TaskStatus.SUCCESS
                        && task.getStatus() != TaskStatus.FAIL
                        && task.getStatus() != TaskStatus.CREATED)
                .collect(Collectors.toList());
        if (notRunningTasks.isEmpty()) {
            return;
        }
        batchPersistTaskStatus(notRunningTasks, TaskStatus.PAUSED);
        updateWithoutPersistWatcher(notRunningTasks, TaskStatus.PAUSED);

    }

    private void updateWithoutPersistWatcher(List<Task> tasks, TaskStatus taskStatus) {
        CompletableFuture.runAsync(() -> {
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForPersist.class));
            tasks.forEach(task -> task.updateStatus(taskStatus));
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
        });
    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;

    private void batchPersistTaskStatus(Collection<Task> tasks, TaskStatus taskStatus) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        //save to db
        List<Long> taskIds = tasks.parallelStream().map(Task::getId).collect(
                Collectors.toList());
        BatchOperateHelper.doBatch(
                taskIds,
                taskStatus,
                (tsks, status) -> taskMapper.updateTaskStatus(new ArrayList<>(tsks), status),
                MAX_BATCH_SIZE);
    }

    /**
     * jobStatus PAUSED->RUNNING; taskStatus PAUSED->CREATED jobStatus FAILED->RUNNING; taskStatus PAUSED->CREATED
     */
    public void resumeJob(String jobUrl) {
        Long jobId = jobManager.getJobId(jobUrl);
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if (null == jobEntity) {
            throw new SwValidationException(ValidSubject.JOB, "job not exists");
        }
        if (jobEntity.getJobStatus() != JobStatus.PAUSED
                && jobEntity.getJobStatus() != JobStatus.FAIL
                && jobEntity.getJobStatus() != JobStatus.CANCELED) {
            throw new SwValidationException(ValidSubject.JOB, "only failed/paused/canceled job can be resumed ");
        }
        Job job = jobBoConverter.fromEntity(jobEntity);
        job = jobLoader.load(job, true);
        jobUpdateHelper.updateJob(job);
    }

}
